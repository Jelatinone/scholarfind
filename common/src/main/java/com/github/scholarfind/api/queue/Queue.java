package com.github.scholarfind.api.queue;

import lombok.NonNull;

public interface Queue<T> extends AutoCloseable {

  QueueResult<T> poll(int messageCount);

  void send(@NonNull T message);
}
