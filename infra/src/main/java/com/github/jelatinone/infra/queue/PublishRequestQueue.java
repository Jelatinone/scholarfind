package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SqsQueue;
import com.github.jelatinone.infra.aws.serial.JacksonSqsSerializer;
import com.github.jelatinone.models.publish.PublishRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class PublishRequestQueue extends SqsQueue<PublishRequest> {
  public PublishRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, new JacksonSqsSerializer<>(PublishRequest.class));
  }
}
