package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SqsQueue;
import com.github.jelatinone.infra.aws.jackson.JacksonSqsSerializer;
import com.github.jelatinone.models.annotate.AnnotateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class AnnotateRequestQueue extends SqsQueue<AnnotateRequest> {
  public AnnotateRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, new JacksonSqsSerializer<>(AnnotateRequest.class));
  }
}
