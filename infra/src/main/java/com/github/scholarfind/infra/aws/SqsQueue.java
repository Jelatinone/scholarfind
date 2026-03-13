package com.github.scholarfind.infra.aws;

import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.event.Level;

import com.github.scholarfind.api.queue.Acknowledgement;
import com.github.scholarfind.api.queue.Queue;
import com.github.scholarfind.api.queue.QueueResult;
import com.github.scholarfind.api.queue.QueueState;
import com.github.scholarfind.api.queue.ReceivedMessage;
import com.github.scholarfind.infra.aws.serial.SqsSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SqsQueue<T> implements Queue<T> {
  SqsClient client;
  String inputUrl;
  String retryUrl;
  String errorUrl;
  SqsSerializer<T> serializer;
  BiConsumer<String, Level> logger;

  @Override
  public QueueResult<T> poll(int messageCount) {
    ReceiveMessageResponse response = client.receiveMessage(
        ReceiveMessageRequest.builder()
            .queueUrl(inputUrl)
            .maxNumberOfMessages(messageCount)
            .messageAttributeNames(".*")
            .build());
    logHttp("Receive message", response.sdkHttpResponse());
    List<ReceivedMessage<T>> messages = response.messages().stream()
        .map(this::wrap)
        .filter(java.util.Objects::nonNull)
        .toList();
    return new QueueResult<>(messages, resolve());
  }

  @Override
  public void send(T message) {
    send(inputUrl, message);
  }

  public void send(String queueUrl, T message) {
    try {
      String body = serializer.encodeBody(message);
      Map<String, String> attributes = serializer.encodeAttributes(message);
      SendMessageResponse response = client.sendMessage(builder -> builder
          .queueUrl(queueUrl)
          .messageBody(body)
          .messageAttributes(encodeAttributes(attributes)));
      logHttp("Send message", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode queue message", exception);
    }
  }

  private ReceivedMessage<T> wrap(Message message) {
    try {
      T decoded = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));
      Acknowledgement acknowledgement = new Acknowledgement() {
        @Override
        public void success() {
          delete(message.receiptHandle());
        }

        @Override
        public void retry() {
          if (retryUrl != null) {
            send(retryUrl, decoded);
          }
          delete(message.receiptHandle());
        }

        @Override
        public void error() {
          if (errorUrl != null) {
            send(errorUrl, decoded);
          }
          delete(message.receiptHandle());
        }
      };
      return new ReceivedMessage<>(decoded, acknowledgement);
    } catch (Exception exception) {
      logger.accept(String.format("Decode queue message failed : %s", exception.getMessage()), Level.ERROR);
      delete(message.receiptHandle());
      return null;
    }
  }

  private void delete(String receiptHandle) {
    DeleteMessageResponse response = client.deleteMessage(builder -> builder
        .queueUrl(inputUrl)
        .receiptHandle(receiptHandle));
    logHttp("Delete message", response.sdkHttpResponse());
  }

  private QueueState resolve() {
    GetQueueAttributesResponse response = client.getQueueAttributes(builder -> builder
        .queueUrl(inputUrl)
        .attributeNames(
            APPROXIMATE_NUMBER_OF_MESSAGES,
            APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
            APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
    logHttp("Resolve queue state", response.sdkHttpResponse());
    boolean queueAlive = response.attributes().values().stream()
        .mapToInt(Integer::parseInt)
        .anyMatch(value -> value > 0);
    return queueAlive ? QueueState.IDLE : QueueState.EMPTY;
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

  private void logHttp(String action, SdkHttpResponse response) {
    if (response == null) {
      return;
    }
    Level level = response.isSuccessful() ? Level.INFO : Level.ERROR;
    logger.accept(
        String.format("%s completed : [%d] %s", action, response.statusCode(), response.statusText()),
        level);
  }

  @Override
  public void close() {
    client.close();
  }
}
