package com.github.jelatinone.api.queue;

import lombok.NonNull;

public interface RetryableQueue<T> extends Queue<T> {

  void sendRetry(@NonNull T message);

  void sendError(@NonNull T message);
}
