package com.github.jelatinone.api.queue;

import java.util.List;

import com.github.jelatinone.api.Envelope;

public record QueueResult<T>(
		List<Envelope<T>> messages,
		QueueState state) {

}
