package com.github.jelatinone.api.queue;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Queryable;

import lombok.NonNull;

public interface Queue<Message, Identifier>
    extends Queryable<Criteria<Identifier>, Message>, AutoCloseable {

  void queue(@NonNull Identifier identifier, @NonNull Message message);
}
