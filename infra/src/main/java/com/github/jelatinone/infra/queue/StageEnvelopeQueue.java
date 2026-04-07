package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.SQSQueue;
import com.github.jelatinone.infra.aws.jackson.JacksonSQSSerializer;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.StageEnvelope;

import software.amazon.awssdk.services.sqs.SqsClient;

public class StageEnvelopeQueue<T extends Request> extends SQSQueue<StageEnvelope<T>> {
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
        new JacksonSQSSerializer<>(
            JacksonMapper.mapper.getTypeFactory().constructParametricType(StageEnvelope.class, payloadType)));
  }
}
