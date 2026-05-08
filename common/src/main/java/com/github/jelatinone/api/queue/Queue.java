package com.github.jelatinone.api.queue;

import com.github.jelatinone.api.Queryable;

import lombok.NonNull;

public interface Queue<Message, Identifier>
		extends Queryable<QueueCriteria<Identifier>, Message>, AutoCloseable {

	void queue(@NonNull Message message);
}
