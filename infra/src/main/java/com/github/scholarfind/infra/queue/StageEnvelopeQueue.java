package com.github.scholarfind.infra.queue;

import com.github.scholarfind.infra.JacksonMapper;
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
      Class<T> payloadType) {
    super(
        client,
        inputUrl,
        retryUrl,
        errorUrl,
        new JacksonSqsSerializer<>(
            JacksonMapper.mapper.getTypeFactory().constructParametricType(StageEnvelope.class, payloadType)));
  }
}
