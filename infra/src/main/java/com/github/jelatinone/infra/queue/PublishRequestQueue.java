package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SQSQueue;
import com.github.jelatinone.infra.aws.jackson.JacksonSQSSerializer;
import com.github.jelatinone.models.publish.PublishRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class PublishRequestQueue extends SQSQueue<PublishRequest> {
  public PublishRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, new JacksonSQSSerializer<>(PublishRequest.class));
  }
}
