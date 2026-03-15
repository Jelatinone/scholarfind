package com.github.scholarfind.infra.queue;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.infra.aws.JacksonMapper;
import com.github.scholarfind.infra.aws.SqsQueue;
import com.github.scholarfind.infra.aws.serial.JacksonSqsSerializer;
import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.StageEnvelope;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class StageEnvelopeQueue<T extends Request> extends SqsQueue<StageEnvelope<T>> {
  public StageEnvelopeQueue(
      SqsClient client,
      String inputUrl,
      String retryUrl,
      String errorUrl,
      Class<T> payloadType,
      BiConsumer<String, Level> logger) {
    super(
        client,
        inputUrl,
        retryUrl,
        errorUrl,
        new JacksonSqsSerializer<>(
            JacksonMapper.mapper.getTypeFactory().constructParametricType(StageEnvelope.class, payloadType)),
        logger);
  }
}
