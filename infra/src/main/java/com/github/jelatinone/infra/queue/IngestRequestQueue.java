package com.github.jelatinone.infra.queue;

import com.github.jelatinone.models.ingest.IngestRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class IngestRequestQueue extends StageEnvelopeQueue<IngestRequest> {
  public IngestRequestQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl) {
    super(client, inputUrl, retryUrl, errorUrl, IngestRequest.class);
  }
}
