package com.github.jelatinone.utility.scheduler;

import java.util.concurrent.ThreadLocalRandom;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LinearBackoffScheduler implements BackoffScheduler {

	Long _baseBackoff;
	Long _maxBackoff;

	Long _factor;

	@NonFinal
	Long currentBackoff;
	@NonFinal
	Integer attempts;

	@Override
	public synchronized long compute() {
		long next = currentBackoff = Math.min(_maxBackoff, _baseBackoff + attempts * _factor);
		long jitter = ThreadLocalRandom.current()
				.nextLong(_baseBackoff, next + 1);
		currentBackoff = jitter;

		return currentBackoff;
	}

	@Override
	public synchronized long compute(int step) {
		long next = currentBackoff = Math.min(_maxBackoff, _baseBackoff + (attempts = step) * _factor);
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
