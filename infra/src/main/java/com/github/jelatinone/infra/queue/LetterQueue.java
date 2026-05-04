package com.github.jelatinone.infra.queue;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.queue.SQSQueue;
import com.github.jelatinone.infra.aws.queue.serial.JacksonSQSSerializer;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

public class LetterQueue<T extends Request> extends SQSQueue<Letter<T>> {
  public LetterQueue(
      SQSQueueDescriptor input,
      SQSQueueDescriptor output,
      SQSQueueDescriptor retry,
      SQSQueueDescriptor error,
      Class<T> payloadType) {
    super(
        input,
        output,
        retry,
        error,
        new JacksonSQSSerializer<>(
            JacksonMapper.mapper.getTypeFactory().constructParametricType(Letter.class, payloadType)));
  }
}