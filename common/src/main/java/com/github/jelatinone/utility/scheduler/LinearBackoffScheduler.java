package com.github.jelatinone.utility.scheduler;

import java.util.concurrent.ThreadLocalRandom;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LinearBackoffScheduler implements BackoffScheduler {

	long _baseBackoff;
	long _maxBackoff;

	long _factor;

	@NonFinal
	Long currentBackoff = 0L;
	@NonFinal
	Integer attempts = 0;

	public LinearBackoffScheduler(long baseBackoff, long maxBackoff, long factor) {
		this._baseBackoff = baseBackoff;
		this._maxBackoff = maxBackoff;
		this._factor = factor;
		this.currentBackoff = baseBackoff;
	}

	@Override
	public synchronized long compute() {
		int nextAttempt = attempts;
		long next = currentBackoff = Math.min(_maxBackoff, _baseBackoff + (long) nextAttempt * _factor);
		long jitter = ThreadLocalRandom.current()
				.nextLong(_baseBackoff, next + 1);
		currentBackoff = jitter;
		attempts = nextAttempt + 1;

		return currentBackoff;
	}

	@Override
	public synchronized long compute(int step) {
		int nextAttempt = Math.max(0, step);
		long next = currentBackoff = Math.min(_maxBackoff, _baseBackoff + (long) nextAttempt * _factor);
		long jitter = ThreadLocalRandom.current().nextLong(_baseBackoff, next + 1);
		currentBackoff = jitter;
		attempts = nextAttempt + 1;

		return currentBackoff;
	}

	@Override
	public synchronized long reset() {
		long previousBackoff = currentBackoff;
		currentBackoff = _baseBackoff;
		attempts = 0;

		return previousBackoff;
	}

	@Override
	public synchronized long acquire() {
		return currentBackoff;
	}
}
