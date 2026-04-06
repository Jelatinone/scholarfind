package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.aws.SQSQueue;
import com.github.jelatinone.infra.aws.jackson.JacksonSQSSerializer;
import com.github.jelatinone.models.ingest.IngestRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class IngestRequestQueue extends SQSQueue<IngestRequest> {
  public IngestRequestQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl) {
    super(client, inputUrl, retryUrl, errorUrl, new JacksonSQSSerializer<>(IngestRequest.class));
  }
}
