package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SQSQueue;
import com.github.jelatinone.infra.aws.jackson.JacksonSQSSerializer;
import com.github.jelatinone.models.annotate.AnnotateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class AnnotateRequestQueue extends SQSQueue<AnnotateRequest> {
  public AnnotateRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, new JacksonSQSSerializer<>(AnnotateRequest.class));
  }
}
