package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SQSQueue;
import com.github.jelatinone.infra.aws.jackson.JacksonSQSSerializer;
import com.github.jelatinone.models.investigate.InvestigateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class InvestigateRequestQueue extends SQSQueue<InvestigateRequest> {
  public InvestigateRequestQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl) {
    super(client, inputUrl, retryUrl, errorUrl, new JacksonSQSSerializer<>(InvestigateRequest.class));
  }
}
