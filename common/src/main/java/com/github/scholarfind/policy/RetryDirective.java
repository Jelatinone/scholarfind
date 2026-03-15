package com.github.scholarfind.policy;

import java.time.Duration;
import java.time.Instant;

public record RetryDirective(
    Duration delay,
    Instant nextAttemptAt) {

  public static RetryDirective delay(Duration delay) {
    return new RetryDirective(delay, null);
  }

  public static RetryDirective at(Instant nextAttemptAt) {
    return new RetryDirective(null, nextAttemptAt);
  }

  public Instant resolve(Instant from) {
    if (nextAttemptAt != null) {
      return nextAttemptAt;
    }
    if (delay != null) {
      return from.plus(delay);
    }
    return null;
  }
}
