package com.github.jelatinone.utility.scheduler;

import java.util.concurrent.ThreadLocalRandom;

import lombok.experimental.NonFinal;

public final class LinearBackoffScheduler implements BackoffScheduler {

  Long _baseBackoff;
  Long _maxBackoff;

  Long _factor;

  @NonFinal
  Long currentBackoff;
  @NonFinal
  Integer attempts;

  public LinearBackoffScheduler(final long baseBackoff, final long boundBackoff, final long growthFactor) {
    _baseBackoff = baseBackoff;
    _maxBackoff = boundBackoff;

    _factor = growthFactor;

    currentBackoff = _baseBackoff;
  }

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
