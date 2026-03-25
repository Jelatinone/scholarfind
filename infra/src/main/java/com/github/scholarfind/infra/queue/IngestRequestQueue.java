package com.github.scholarfind.infra.queue;



import com.github.scholarfind.infra.aws.SqsQueue;
import com.github.scholarfind.infra.aws.serial.JacksonSqsSerializer;
import com.github.scholarfind.models.ingest.IngestRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class IngestRequestQueue extends SqsQueue<IngestRequest> {
  public IngestRequestQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl) {
    super(client, inputUrl, retryUrl, errorUrl, new JacksonSqsSerializer<>(IngestRequest.class));
  }
}
