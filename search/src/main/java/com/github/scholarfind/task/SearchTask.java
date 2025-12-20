package com.github.scholarfind.task;

import static org.slf4j.event.Level.*;
import static com.github.scholarfind.meta.Post.*;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.meta.Collect;
import com.github.scholarfind.meta.Post;
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
    String retryQueueName = "queue_retry";

    @Builder.Default
    String errorQueueName = "queue_error";
  }

  static Logger _logger = LoggerFactory.getLogger(SearchTask.class);
  static MetricPublisher _metrics = CloudWatchMetricPublisher.builder()
      .cloudWatchClient(CloudWatchAsyncClient.create())
      .detailedMetrics(CoreMetric.API_CALL_DURATION)
      .build();

  ObjectMapper _mapper;
  SqsClient _queueClient;

  String _inQueueUrl;
  String _outQueueUrl;
  String _retryQueueUrl;
  String _errorQueueUrl;

  Configuration _searchConfig;

  public SearchTask(Task.Configuration taskConfig, SearchTask.Configuration searchConfig) {
    super(taskConfig);
    _searchConfig = searchConfig;

    _mapper = new ObjectMapper()
        .configure(Feature.ALLOW_COMMENTS, true)
        .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    _queueClient = SqsClient.builder()
        .overrideConfiguration(config -> config.addMetricPublisher(_metrics))
        .build();
    useMessage("Initialized resources : queue client", INFO);

    _inQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.inQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource : ingestion queue URL", INFO);

    _outQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.outQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource : output queue URL", INFO);

    _retryQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.retryQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource : retry queue URL", INFO);

    _errorQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.errorQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource : error queue URL", INFO);
  }

  private void send(@NonNull String queueUrl, @NonNull String body,
      @NonNull Map<String, MessageAttributeValue> attributes) {
    SendMessageRequest sendRequest = SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageAttributes(attributes)
        .messageBody(body)
        .build();
    SendMessageResponse sendResponse = _queueClient.sendMessage(sendRequest);
    useMessage(String.format("Queued to %s [MessageId=%s]", queueUrl, sendResponse.messageId()), INFO);
  }

  private SearchDocument parse(final @NonNull Message message) {
    Map<String, MessageAttributeValue> attributes = message.messageAttributes();
    String body = message.body();

    SearchDocument document = null;
    try {
      document = SearchDocument.parse(message);
      useMessage(String.format("Parse successfully from [MessageId=%s]", message.messageId()), INFO);
      return document;
    } catch (final MalformedURLException exception) {
      send(_errorQueueUrl, body, attributes);
    } catch (final IOException exception) {
      send(_retryQueueUrl, body, attributes);
    }
    useMessage(String.format("Parse failed from [MessageId=%s]", message.messageId()), ERROR);
    return document;
  }

  @Override
  public void close() throws IOException {
    _queueClient.close();
  }

  @Override
  protected @NonNull Collect<@NonNull SearchDocument> collect() {
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

    List<SearchDocument> searchDocuments = collectionMessages.stream()
        .map(this::parse)
        .filter(Objects::nonNull)
        .toList();
    useMessage(
        String.format("Collection valid results size : %d", searchDocuments.size()),
        INFO);

    Collect<SearchDocument> result;
    if (collectionMessages.size() > 0) {
      result = new Collect.Alive<SearchDocument>(searchDocuments);
    } else {
      result = queueAlive
          ? new Collect.Idle<>()
          : new Collect.Empty<>();
    }

    return result;
  }

  @Override
  protected @NonNull SearchDocument operate(final @NonNull SearchDocument operand) {
    return null;
  }

  @Override
  protected Post post(final SearchDocument operand) {
    Map<String, MessageAttributeValue> attributes = operand.extract();
    try {
      String body = _mapper.writeValueAsString(operand);
      send(_outQueueUrl, body, attributes);
      return SUCCESS;
    } catch (final JsonMappingException exception) {
      return FAILURE_FATAL;
    } catch (final IOException exception) {
      return FAILURE_RETRY;
    }
  }
}
