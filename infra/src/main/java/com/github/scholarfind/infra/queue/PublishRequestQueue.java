package com.github.scholarfind.infra.queue;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.infra.aws.SqsQueue;
import com.github.scholarfind.infra.aws.serial.JacksonSqsSerializer;
import com.github.scholarfind.models.publish.PublishRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class PublishRequestQueue extends SqsQueue<PublishRequest> {
  public PublishRequestQueue(
      SqsClient client,
      String queueUrl,
      BiConsumer<String, Level> logger) {
    super(client, queueUrl, null, null, new JacksonSqsSerializer<>(PublishRequest.class), logger);
  }
}
