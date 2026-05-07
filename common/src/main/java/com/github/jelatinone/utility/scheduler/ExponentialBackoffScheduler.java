package com.github.jelatinone.utility.scheduler;

import java.util.concurrent.ThreadLocalRandom;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ExponentialBackoffScheduler implements BackoffScheduler {

	long _baseBackoff;
	long _maxBackoff;

	long _factor;

	@NonFinal
	long currentBackoff;

	public ExponentialBackoffScheduler(long baseBackoff, long maxBackoff, long factor) {
		this._baseBackoff = baseBackoff;
		this._maxBackoff = maxBackoff;
		this._factor = factor;
		this.currentBackoff = baseBackoff;
	}

	@Override
	public synchronized long compute() {
		long next = Math.max(_baseBackoff, Math.min(_maxBackoff, currentBackoff * _factor));
		long jitter = ThreadLocalRandom.current()
				.nextLong(_baseBackoff, next + 1);
		currentBackoff = jitter;

		return currentBackoff;
	}

	@Override
	public synchronized long compute(int step) {
		long next = _baseBackoff;
		for (int attempt = 0; attempt < Math.max(0, step); attempt++) {
			next = Math.max(_baseBackoff, Math.min(_maxBackoff, next * _factor));
		}
		long jitter = ThreadLocalRandom.current()
				.nextLong(_baseBackoff, next + 1);
		currentBackoff = jitter;

		return currentBackoff;
	}

	@Override
	public synchronized long reset() {
		long previousBackoff = currentBackoff;
		currentBackoff = _baseBackoff;

		return previousBackoff;
	}

	@Override
	public synchronized long acquire() {
		return currentBackoff;
	}
}
