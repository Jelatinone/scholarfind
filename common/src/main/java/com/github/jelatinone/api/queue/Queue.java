package com.github.jelatinone.api.queue;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Queryable;

import lombok.NonNull;

public interface Queue<Identifier, Message, Criterion extends Criteria<Identifier>>
    extends Queryable<Criterion, QueueEnvelope<Message>>, AutoCloseable {

  void queue(@NonNull Criterion criteria, @NonNull Message message);
}
