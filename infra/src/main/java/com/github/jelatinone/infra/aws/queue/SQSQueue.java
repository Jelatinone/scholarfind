package com.github.jelatinone.infra.aws.queue;

import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.Acknowledgement;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.QueueState;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.infra.aws.queue.serial.SQSSerializer;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SQSQueue<Value> implements RetryableQueue<Value> {
  SQSQueueDescriptor input, output, retry, error;
  SQSSerializer<Value> serializer;

  static Logger _logger = LoggerFactory.getLogger(SQSQueue.class);

  @Override
  public QueueResult<Value> poll(int messageCount) {
    ReceiveMessageResponse response = input.client().receiveMessage(
        ReceiveMessageRequest.builder()
            .queueUrl(input.queueUrl())
            .maxNumberOfMessages(messageCount)
            .messageAttributeNames(".*")
            .build());
    logResponse("Receive message", response.sdkHttpResponse());

    List<Message> messages = response.messages();
    List<QueueEnvelope<Value>> envelopes = messages.stream()
        .map(this::wrap)
        .filter(java.util.Objects::nonNull)
        .toList();
    return new QueueResult<>(envelopes, resolve(messages));
  }

  @Override
  public void send(Value message) {
    send(output, message);
  }

  @Override
  public void sendRetry(@NonNull Value message) {
    send(retry, message);
  }

  @Override
  public void sendError(@NonNull Value message) {
    send(error, message);
  }

  public void send(SQSQueueDescriptor queue, Value message) {
    if (queue.queueUrl() == null) {
      throw new IllegalStateException("Error queue URL is not configured");
    }
    try {
      String body = serializer.encodeBody(message);
      Map<String, String> attributes = serializer.encodeAttributes(message);
      SendMessageResponse response = queue.client().sendMessage(builder -> builder
          .queueUrl(queue.queueUrl())
          .messageBody(body)
          .messageAttributes(encodeAttributes(attributes)));
      logResponse("Send message", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode queue message", exception);
    }
  }

  public void send(SQSQueueDescriptor queue, String body, Map<String, MessageAttributeValue> attributes) {
    if (queue.queueUrl() == null) {
      throw new IllegalStateException("Error queue URL is not configured");
    }
    try {
      SendMessageResponse response = queue.client().sendMessage(builder -> builder
          .queueUrl(queue.queueUrl())
          .messageBody(body)
          .messageAttributes(attributes));
      logResponse("Send message", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode queue message", exception);
    }
  }

  private QueueEnvelope<Value> wrap(Message message) {
    try {
      Value decoded = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));
      Acknowledgement acknowledgement = new Acknowledgement() {
        @Override
        public void success() {
          // Do nothing ;)
        }

        @Override
        public void retry() {
          send(retry, decoded);
        }

        @Override
        public void error() {
          send(error, decoded);

        }
      };
      return new QueueEnvelope<>(decoded, acknowledgement);
    } catch (Exception exception) {
      _logger.error(String.format("Decode queue message failed : %s", exception.getMessage()));
      send(error, message.body(), message.messageAttributes());
      return null;
    } finally {
      delete(message.receiptHandle());
    }
  }

  private void delete(String receiptHandle) {
    DeleteMessageResponse response = input.client().deleteMessage(builder -> builder
        .queueUrl(input.queueUrl())
        .receiptHandle(receiptHandle));
    logResponse("Delete message", response.sdkHttpResponse());
  }

  private QueueState resolve(List<Message> messages) {
    if (!messages.isEmpty()) {
      return QueueState.ACTIVE;
    }
    GetQueueAttributesResponse response = input.client().getQueueAttributes(builder -> builder
        .queueUrl(input.queueUrl())
        .attributeNames(
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
    logResponse("Resolve queue state", response.sdkHttpResponse());
    boolean containsAnyMessages = response.attributes().values().stream()
        .mapToInt(Integer::parseInt)
        .anyMatch(value -> value > 0);
    return containsAnyMessages ? QueueState.IDLE : QueueState.EMPTY;
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

  private void logResponse(String action, SdkHttpResponse response) {
    if (response == null) {
      return;
    }
    String message = String.format("%s completed : [%d] %s", action, response.statusCode(), response.statusText());
    if (response.isSuccessful()) {
      _logger.info(message);
      return;
    }
    _logger.error(message);
  }

  @Override
  public void close() {
    input.client().close();
    output.client().close();
    retry.client().close();
    error.client().close();
  }

  public static record SQSQueueDescriptor(SqsClient client, String queueUrl) {
  }
}
