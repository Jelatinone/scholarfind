package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SqsQueue;
import com.github.jelatinone.infra.aws.serial.JacksonSqsSerializer;
import com.github.jelatinone.models.investigate.InvestigateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class InvestigateRequestQueue extends SqsQueue<InvestigateRequest> {
  public InvestigateRequestQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl) {
    super(client, inputUrl, retryUrl, errorUrl, new JacksonSqsSerializer<>(InvestigateRequest.class));
  }
}
