package com.github.jelatinone.meta.archetype.queue;

import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.Retrieve;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueOperation<Consumes, Produces> implements Operate<QueueEnvelope<Consumes>, QueueEnvelope<Produces>> {

  Operate<Consumes, Produces> operation;
  Retrieve<Consumes, Produces> recovery;

  @Override
  public QueueEnvelope<Produces> operate(QueueEnvelope<Consumes> operand) {
    try {
      Produces output = operation.operate(operand.content());
      return new QueueEnvelope<>(output, operand.acknowledgement());
    } catch (Throwable throwable) {
      Produces recovered = recovery.recover(operand.content(), throwable);
      return new QueueEnvelope<>(recovered, operand.acknowledgement());
    }
  }

}
