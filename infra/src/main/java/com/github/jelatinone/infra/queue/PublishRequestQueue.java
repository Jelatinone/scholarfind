package com.github.jelatinone.infra.queue;

import com.github.jelatinone.models.publish.PublishRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class PublishRequestQueue extends StageEnvelopeQueue<PublishRequest> {
  public PublishRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, PublishRequest.class);
  }
}
