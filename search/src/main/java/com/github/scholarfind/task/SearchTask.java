package com.github.scholarfind.task;

import static org.slf4j.event.Level.*;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.meta.CollectionResult;
import com.github.scholarfind.meta.SequentialTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.document.search.SearchDocument;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask extends SequentialTask<SearchDocument, SearchDocument> {

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {

    @Builder.Default
    String inQueueName = "queue_search";

    @Builder.Default
    String outQueueName = "queue_annotate";

    @Builder.Default
    String dlqQueueName = "queue_dlq";
  }

  static Logger _logger = LoggerFactory.getLogger(SearchTask.class);
  static MetricPublisher _metrics = CloudWatchMetricPublisher.builder()
      .cloudWatchClient(CloudWatchAsyncClient.create())
      .detailedMetrics(CoreMetric.API_CALL_DURATION)
      .build();

  SqsClient _queueClient;
  CloudWatchClient _monitorClient;

  ObjectMapper _mapper;

  String _inQueueUrl;
  String _outQueueUrl;

  Configuration _searchConfig;

  public SearchTask(Task.Configuration taskConfig, SearchTask.Configuration searchConfig) {
    super(taskConfig);
    _searchConfig = searchConfig;

    _mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    _queueClient = SqsClient.builder()
        .overrideConfiguration(config -> config.addMetricPublisher(_metrics))
        .build();
    useMessage("Initialized resources : queue client", INFO);

    _monitorClient = CloudWatchClient.create();
    useMessage("Initialized resources : monitor client", INFO);

    _inQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.inQueueName)
            .build())
        .queueUrl();
    _outQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.outQueueName)
            .build())
        .queueUrl();
  }

  private void sendQueuePayload(@NonNull String payload, @NonNull String queueUrl,
      @NonNull Map<String, MessageAttributeValue> attributes) {
    SendMessageRequest sendRequest = SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageAttributes(attributes)
        .messageBody(payload)
        .build();
    SendMessageResponse sendResponse = _queueClient.sendMessage(sendRequest);
    useMessage(String.format("Re-queued payload to %s [MessageId=%s]", queueUrl, sendResponse.messageId()), INFO);
  }

  private SearchDocument parseMessage(@NonNull Message message) {
    final String payload = message.body();
    try {
      SearchDocument document = _mapper.readValue(payload, SearchDocument.class);
      Integer attempts = document.trace().attempt();

      if (attempts > _taskConfig.operandRetries) {
        useMessage(
            "Document exceeds local maximum retries",
            ERROR);
        sendQueuePayload(payload, _searchConfig.dlqQueueName, message.messageAttributes());
        return null;
      }

      return document;
    } catch (final JsonParseException exception) {
      useMessage(
          "JSON format failure",
          ERROR);
      sendQueuePayload(payload, _searchConfig.dlqQueueName, message.messageAttributes());
      return null;
    } catch (final IOException exception) {
      useMessage(
          "Transfient payload failure",
          WARN);
      sendQueuePayload(payload, _searchConfig.inQueueName, message.messageAttributes());
      return null;
    }
  }

  @Override
  public void close() throws Exception {
    _queueClient.close();
    _monitorClient.close();
  }

  @Override
  protected @NonNull CollectionResult<@NonNull SearchDocument> collect() {
    ReceiveMessageRequest collectionRequest = ReceiveMessageRequest.builder()
        .queueUrl(_inQueueUrl)
        .maxNumberOfMessages(_taskConfig.collectionSize)
        .build();
    List<Message> collectionMessages = _queueClient.receiveMessage(collectionRequest).messages();
    useMessage(
        String.format("Collection results size : %d", collectionMessages.size()),
        INFO);

    GetQueueAttributesRequest attributesRequest = GetQueueAttributesRequest
        .builder()
        .queueUrl(_inQueueUrl)
        .attributeNames(APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED, APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
            APPROXIMATE_NUMBER_OF_MESSAGES)
        .build();
    Map<QueueAttributeName, String> queueAttributes = _queueClient.getQueueAttributes(attributesRequest).attributes();
    boolean queueAlive = queueAttributes.values()
        .stream()
        .allMatch((value) -> Integer.valueOf(value) != 0);
    useMessage(
        String.format("Collection queue metrics alive : %s", queueAlive),
        INFO);

    CollectionResult<SearchDocument> result;
    if (collectionMessages.size() > 0) {
      List<SearchDocument> searchDocuments = collectionMessages.stream()
          .map(this::parseMessage)
          .filter(Objects::nonNull)
          .toList();
      useMessage(
          String.format("Valid collection results size : %d", searchDocuments.size()),
          INFO);

      result = new CollectionResult.Alive<SearchDocument>(searchDocuments);
      useMessage(
          "Collection result shape : ALIVE",
          INFO);
    } else {
      if (queueAlive) {
        result = new CollectionResult.Idle<>();
        useMessage(
            "Collection result shape : IDLE",
            INFO);
      } else {
        result = new CollectionResult.Empty<>();
        useMessage(
            "Collection result shape : EMPTY",
            INFO);
      }
    }

    return result;
  }

  @Override
  protected @NonNull SearchDocument operate(@NonNull SearchDocument operand) {
    throw new UnsupportedOperationException("Unimplemented method 'operate'");
  }

  @Override
  protected boolean post(SearchDocument operand) {
    String body = null;

    // TODO: Repair Instances...?
    Map<String, MessageAttributeValue> attributes = new HashMap<>();
    attributes.put("origin", MessageAttributeValue.builder().stringValue(_taskConfig._name).build());
    attributes.put("depth", MessageAttributeValue.builder().stringValue(operand.trace().depth().toString()).build());
    attributes.put("attempts",
        MessageAttributeValue.builder().stringValue(operand.trace().attempt().toString()).build());
    attributes.put("reason", MessageAttributeValue.builder().stringValue(operand.reason().toString()).build());
    attributes.put("url",
        MessageAttributeValue.builder().stringValue(operand.trace().url().toString()).build());

    try {
      body = _mapper.writeValueAsString(operand);
      SendMessageRequest sendRequest = SendMessageRequest.builder()
          .queueUrl(_outQueueUrl)
          .messageBody(body)
          .messageAttributes(attributes)
          .build();
      _queueClient.sendMessage(sendRequest);
      return true;
    } catch (final JsonGenerationException exception) {
      useMessage(
          String.format("JSON format failure : sent to dead letter queue"),
          ERROR);
      sendQueuePayload(body != null ? body : "", _searchConfig.dlqQueueName, attributes);
    } catch (final IOException exception) {
      useMessage(
          String.format("Transfient payload failure : sent to retry queue"),
          WARN);
      sendQueuePayload(body != null ? body : "", _searchConfig.inQueueName, attributes);
    }
    return false;
  }
}
