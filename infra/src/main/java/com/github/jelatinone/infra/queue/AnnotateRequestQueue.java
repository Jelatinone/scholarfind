package com.github.jelatinone.infra.queue;

import com.github.jelatinone.models.annotate.AnnotateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class AnnotateRequestQueue extends StageEnvelopeQueue<AnnotateRequest> {
  public AnnotateRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, AnnotateRequest.class);
  }
}
