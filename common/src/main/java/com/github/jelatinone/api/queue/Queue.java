package com.github.jelatinone.api.queue;

import com.github.jelatinone.api.Queryable;

import lombok.NonNull;

public interface Queue<Message, Identifier>
    extends Queryable<Identifier, Message>, AutoCloseable {

  void queue(@NonNull Identifier identifier, @NonNull Message message);
}
