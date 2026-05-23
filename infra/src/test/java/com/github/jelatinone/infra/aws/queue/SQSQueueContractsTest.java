package com.github.jelatinone.infra.aws.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.infra.aws.queue.serial.SQSSerializer;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

class SQSQueueContractsTest {

  private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/test";

  @Test
  void queue_encodesRawMessagesWithSendCriteria() {
    RecordingSqsClient client = new RecordingSqsClient();
    SQSQueue<String> queue = new SQSQueue<>(client, new StringSerializer());
    SQSCriteria.Send send = SQSCriteria.send(QUEUE_URL).asSend()
        .withDeduplicationId("dedupe-1")
        .withGroupId("group-1");

    queue.queue(send, "payload");

    SendMessageRequest request = client.sent.get(0);
    assertEquals(QUEUE_URL, request.queueUrl());
    assertEquals("body:payload", request.messageBody());
    assertEquals("attribute:payload", request.messageAttributes().get("kind").stringValue());
    assertEquals("dedupe-1", request.messageDeduplicationId());
    assertEquals("group-1", request.messageGroupId());
  }

  @Test
  void singularQueryReturnsEnvelopeAndDeletesOnlyWhenAcknowledged() {
    RecordingSqsClient client = new RecordingSqsClient(
        List.of(message("payload", "receipt-1", Map.of("kind", "received"))));
    SQSQueue<String> queue = new SQSQueue<>(client, new StringSerializer());
    SQSCriteria.Receive receive = SQSCriteria.receive(QUEUE_URL).asReceive()
        .withMessageAttributeNames(List.of("kind"));

    QueueEnvelope<String> envelope = queue.query(new Singular<>(receive)).orElseThrow();

    assertEquals("payload:received", envelope.content());
    assertEquals(QUEUE_URL, client.received.get(0).queueUrl());
    assertEquals(List.of("kind"), client.received.get(0).messageAttributeNames());
    assertTrue(client.deleted.isEmpty());

    envelope.acknowledgement().retry();

    assertTrue(client.deleted.isEmpty());

    envelope.acknowledgement().success();

    assertEquals(1, client.deleted.size());
    assertEquals(QUEUE_URL, client.deleted.get(0).queueUrl());
    assertEquals("receipt-1", client.deleted.get(0).receiptHandle());
  }

  private static Message message(String body, String receiptHandle, Map<String, String> attributes) {
    return Message.builder()
        .body(body)
        .receiptHandle(receiptHandle)
        .messageAttributes(attributes.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(entry.getValue())
                    .build())))
        .build();
  }

  private static SdkHttpResponse ok() {
    return SdkHttpResponse.builder()
        .statusCode(200)
        .statusText("OK")
        .build();
  }

  private static final class RecordingSqsClient implements SqsClient {
    final ArrayDeque<Collection<Message>> messageBatches = new ArrayDeque<>();
    final List<SendMessageRequest> sent = new ArrayList<>();
    final List<ReceiveMessageRequest> received = new ArrayList<>();
    final List<DeleteMessageRequest> deleted = new ArrayList<>();

    @SafeVarargs
    private RecordingSqsClient(Collection<Message>... messageBatches) {
      this.messageBatches.addAll(List.of(messageBatches));
    }

    @Override
    public SendMessageResponse sendMessage(Consumer<SendMessageRequest.Builder> request) {
      SendMessageRequest.Builder builder = SendMessageRequest.builder();
      request.accept(builder);
      sent.add(builder.build());
      SendMessageResponse.Builder response = SendMessageResponse.builder()
          .messageId("message-id");
      response.sdkHttpResponse(ok());
      return response.build();
    }

    @Override
    public ReceiveMessageResponse receiveMessage(Consumer<ReceiveMessageRequest.Builder> request) {
      ReceiveMessageRequest.Builder builder = ReceiveMessageRequest.builder();
      request.accept(builder);
      received.add(builder.build());
      Collection<Message> messages = messageBatches.isEmpty() ? List.of() : messageBatches.removeFirst();
      ReceiveMessageResponse.Builder response = ReceiveMessageResponse.builder()
          .messages(messages);
      response.sdkHttpResponse(ok());
      return response.build();
    }

    @Override
    public DeleteMessageResponse deleteMessage(Consumer<DeleteMessageRequest.Builder> request) {
      DeleteMessageRequest.Builder builder = DeleteMessageRequest.builder();
      request.accept(builder);
      deleted.add(builder.build());
      DeleteMessageResponse.Builder response = DeleteMessageResponse.builder();
      response.sdkHttpResponse(ok());
      return response.build();
    }

    @Override
    public String serviceName() {
      return SqsClient.SERVICE_NAME;
    }

    @Override
    public void close() {
    }
  }

  private static final class StringSerializer implements SQSSerializer<String> {
    @Override
    public String decode(String body, Map<String, String> attributes) {
      return body + ":" + attributes.getOrDefault("kind", "");
    }

    @Override
    public String encodeBody(String value) {
      return "body:" + value;
    }

    @Override
    public Map<String, String> encodeAttributes(String value) {
      return Map.of("kind", "attribute:" + value);
    }
  }
}
