package com.github.scholarfind.meta;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public final class DelayedValue<Type> implements Delayed {
  Type operand;
  long delayNano;

  DelayedValue(Type operand, long delay, TimeUnit unit) {
    this.operand = operand;
    this.delayNano = System.nanoTime() + unit.toNanos(delay);
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(
        delayNano - System.nanoTime(),
        TimeUnit.NANOSECONDS);
  }

  @Override
  public int compareTo(Delayed other) {
    return Long.compare(
        this.delayNano,
        other.getDelay(TimeUnit.NANOSECONDS));
  }
}
