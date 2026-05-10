package com.github.jelatinone.infra.aws.queue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.QueueException;
import com.github.jelatinone.infra.aws.AWSQueryable;
import com.github.jelatinone.infra.aws.queue.serial.SQSSerializer;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SQSQueue<Value>
    implements Queue<Value, SQSCriteria>, AWSQueryable<CreateQueueRequest, CreateQueueResponse> {

  SqsClient client;
  SQSSerializer<Value> serializer;

  static Logger _logger = LoggerFactory.getLogger(SQSQueue.class);

  @Override
  public Optional<CreateQueueResponse> tryCreate(CreateQueueRequest create) {
    try {
      CreateQueueResponse response = client.createQueue(create);
      response("create queue", response.sdkHttpResponse());
      return Optional.of(response);
    } catch (QueueNameExistsException exception) {
      return Optional.empty();
    } catch (Exception exception) {
      throw new QueueException.RetryQueueException(exception.getMessage(), exception);
    }
  }

  @Override
  public void queue(@NonNull SQSCriteria critiera, @NonNull Value message) {
    try {
      String queueUrl = identifier(critiera);
      String body = serializer.encodeBody(message);
      Map<String, String> attributes = serializer.encodeAttributes(message);
      SendMessageResponse response = client.sendMessage(builder -> builder
          .queueUrl(queueUrl)
          .messageBody(body)
          .messageDeduplicationId(critiera.deduplicationId())
          .messageGroupId(critiera.groupId())
          .messageSystemAttributes(critiera.messageSystemAttributes())
          .messageAttributes(encodeAttributes(attributes)));
      response("send message", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode queue message", exception);
    }
  }

  private void delete(String queueUrl, String receiptHandle) {
    DeleteMessageResponse response = client.deleteMessage(builder -> builder
        .queueUrl(queueUrl)
        .receiptHandle(receiptHandle));
    response("delete message", response.sdkHttpResponse());
  }

  @Override
  public boolean query(Exists<SQSCriteria> query) {
    return query(new Count<>(query.criteria())) > 0;
  }

  @Override
  public long query(Count<SQSCriteria> query) {
    try {
      String queueUrl = identifier(query.criteria());
      GetQueueAttributesResponse response = client.getQueueAttributes(builder -> builder
          .queueUrl(queueUrl)
          .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));

      response("get queue attributes", response.sdkHttpResponse());
      long count = Long
          .parseLong(response.attributes().getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0"));
      return count;
    } catch (Exception exception) {
      _logger.error(String.format("Queue query failed : %s", exception.getMessage()));
      throw new QueueException.RetryQueueException(exception.getMessage(), exception);
    }
  }

  @Override
  public Optional<Value> query(Singular<SQSCriteria> query) {
    try {
      String queueUrl = identifier(query.criteria());
      ReceiveMessageResponse response = client.receiveMessage(
          builder -> builder
              .queueUrl(queueUrl)
              .messageAttributeNames(query.criteria().messageAttributeNames())
              .messageSystemAttributeNames(query.criteria().messageSystemAttributeNames())
              .maxNumberOfMessages(1)
              .messageAttributeNames(".*")
              .build());
      response("receive message", response.sdkHttpResponse());

      Message message = response.messages().stream()
          .filter(java.util.Objects::nonNull)
          .findFirst()
          .orElse(null);
      Value value = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));

      delete(queueUrl, message.receiptHandle());
      return value == null
          ? Optional.of(value)
          : Optional.empty();
    } catch (IOException exception) {
      _logger.error(String.format("Decode queue message failed : %s", exception.getMessage()));
      throw new QueueException.FatalQueueException(exception.getMessage(), exception);
    } catch (Exception exception) {
      _logger.error(String.format("Queue query failed : %s", exception.getMessage()));
      throw new QueueException.RetryQueueException(exception.getMessage(), exception);
    }
  }

  @Override
  public Collection<Value> query(Several<SQSCriteria> query) {
    try {
      String queueUrl = identifier(query.criteria());
      ReceiveMessageResponse response = client.receiveMessage(
          builder -> builder
              .queueUrl(queueUrl)
              .messageAttributeNames(query.criteria().messageAttributeNames())
              .messageSystemAttributeNames(query.criteria().messageSystemAttributeNames())
              .maxNumberOfMessages(query.limit())
              .messageAttributeNames(".*")
              .build());
      response("receive message(s)", response.sdkHttpResponse());

      List<Value> envelopes = response.messages().stream()
          .filter(java.util.Objects::nonNull)
          .map((message) -> {
            Value value;
            try {
              value = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));

              delete(queueUrl, message.receiptHandle());
              return value;
            } catch (IOException exception) {
              _logger.error(String.format("Decode queue message failed : %s", exception.getMessage()));
              throw new QueueException.FatalQueueException(exception.getMessage(), exception);
            }
          }).toList();
      return envelopes;
    } catch (Exception exception) {
      _logger.error(String.format("Queue query failed : %s", exception.getMessage()));
      throw new QueueException.RetryQueueException(exception.getMessage(), exception);
    }
  }

  private Map<String, String> decodeAttributes(Map<String, MessageAttributeValue> attributes) {
    return attributes.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stringValue()));
  }

  private Map<String, MessageAttributeValue> encodeAttributes(Map<String, String> attributes) {
    return attributes.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> MessageAttributeValue.builder().dataType("String").stringValue(entry.getValue()).build()));
  }

  @Override
  public void close() {
    client.close();
  }

  private static String identifier(Criteria<String> critiera) {
    return critiera.identifier().orElseThrow();
  }

  private static void response(String action, SdkHttpResponse response) {
    if (response == null) {
      return;
    }
    String message = String.format("%s [%d] : %s", action, response.statusCode(), response.statusText());
    if (response.isSuccessful()) {
      _logger.info(message);
      return;
    }
    _logger.error(message);
  }
}
