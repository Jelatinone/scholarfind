package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.SQSQueue;
import com.github.jelatinone.infra.aws.serial.jackson.JacksonSQSSerializer;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import software.amazon.awssdk.services.sqs.SqsClient;

public class LetterQueue<T extends Request> extends SQSQueue<Letter<T>> {
  public LetterQueue(
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
            JacksonMapper.mapper.getTypeFactory().constructParametricType(Letter.class, payloadType)));
  }
}