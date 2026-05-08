package com.github.jelatinone.infra.queue;

import com.github.jelatinone.api.queue.Queue;

import lombok.NonNull;

public interface RetryableQueue<T, Queryable> extends Queue<T, Queryable> {

  void retry(@NonNull T message);

  void error(@NonNull T message);
}
