package com.github.jelatinone.task.signal;

public enum SignalTier {
  TRACE,
  METADATA,
  TEXT,
  CONTENT;

  public boolean includes(SignalTier other) {
    return ordinal() >= other.ordinal();
  }
}
