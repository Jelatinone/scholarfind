package com.github.jelatinone.api.queue;

import lombok.NonNull;

public interface RetryableQueue<T, Queryable> extends Queue<QueueEnvelope<T>, Queryable> {

	void retry(@NonNull T message);

	void error(@NonNull T message);
}
