package com.github.scholarfind.utility;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public final class Locked<Type> implements Delayed {
  Type value;
  long delayNano;

  public Locked(Type operand, long delay, TimeUnit unit) {
    this.value = operand;
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
