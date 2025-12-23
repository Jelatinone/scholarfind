package com.github.scholarfind.api;

import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import lombok.NonNull;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public final class QueueHelpers {
  public static SendMessageResponse queueMessage(@NonNull SqsClient client, @NonNull String queueUrl,
      @NonNull String body,
      @NonNull Map<String, MessageAttributeValue> attributes, final @NonNull BiConsumer<String, Level> logger) {
    SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageAttributes(attributes)
        .messageBody(body)
        .build();
    SendMessageResponse sendMessageResponse = client.sendMessage(sendMessageRequest);
    SdkHttpResponse requestSdkResponse = sendMessageResponse.sdkHttpResponse();

    String id = attributes.get("id").stringValue();
    if (requestSdkResponse.isSuccessful()) {
      logger.accept(
          String.format("Queue message to queue [%s] completed : %s", queueUrl, id),
          Level.INFO);
    } else {
      logger.accept(
          String.format("Queue message to queue [%s] failed : %s", queueUrl, id),
          Level.ERROR);
    }

    return sendMessageResponse;
  }
}
