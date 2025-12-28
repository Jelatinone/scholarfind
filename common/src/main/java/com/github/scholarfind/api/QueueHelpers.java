package com.github.scholarfind.api;

import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import lombok.NonNull;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public final class QueueHelpers {

  public static BiConsumer<String, Level> _logger = (f, s) -> {
  };

  public static SendMessageResponse queueMessage(@NonNull SqsClient client, @NonNull String queueUrl,
      @NonNull String body, @NonNull String receiptHandle,
      @NonNull Map<String, MessageAttributeValue> attributes) {
    SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageAttributes(attributes)
        .messageBody(body)
        .build();
    SendMessageResponse sendMessageResponse = client.sendMessage(sendMessageRequest);
    SdkHttpResponse requestSdkResponse = sendMessageResponse.sdkHttpResponse();
    if (requestSdkResponse.isSuccessful()) {
      _logger.accept(
          String.format("Queue message to queue [%s] completed : %s", queueUrl,
              receiptHandle),
          Level.INFO);
    } else {
      _logger.accept(
          String.format("Queue message to queue [%s] failed : %s", queueUrl,
              receiptHandle),
          Level.ERROR);
    }

    return sendMessageResponse;
  }

  public static DeleteMessageResponse deleteMessage(@NonNull SqsClient client, @NonNull String queueUrl,
      @NonNull String receiptHandle) {
    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueUrl)
        .receiptHandle(receiptHandle).build();
    DeleteMessageResponse deleteMessageResponse = client.deleteMessage(deleteMessageRequest);
    SdkHttpResponse requestSdkResponse = deleteMessageResponse.sdkHttpResponse();

    if (requestSdkResponse.isSuccessful()) {
      _logger.accept(
          String.format("Queue message to queue [%s] completed : %s", queueUrl, receiptHandle),
          Level.INFO);
    } else {
      _logger.accept(
          String.format("Queue message to queue [%s] failed : %s", queueUrl, receiptHandle),
          Level.ERROR);
    }

    return deleteMessageResponse;
  }
}
