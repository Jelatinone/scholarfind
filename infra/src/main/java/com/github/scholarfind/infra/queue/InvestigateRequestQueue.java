package com.github.scholarfind.infra.queue;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.infra.aws.SqsQueue;
import com.github.scholarfind.infra.aws.serial.JacksonSqsSerializer;
import com.github.scholarfind.models.investigate.InvestigateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class InvestigateRequestQueue extends SqsQueue<InvestigateRequest> {
  public InvestigateRequestQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl,
      BiConsumer<String, Level> logger) {
    super(client, inputUrl, retryUrl, errorUrl, new JacksonSqsSerializer<>(InvestigateRequest.class), logger);
  }
}
