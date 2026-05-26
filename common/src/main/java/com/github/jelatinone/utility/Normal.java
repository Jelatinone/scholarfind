package com.github.jelatinone.utility;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Normal<@NonNull T extends Number> {

  @NonFinal
  @Getter
  T boundValue;

  @Getter
  T boundUpper, boundLower;

  public Normal(T boundValue, T boundUpper, T boundLower) {
    if (boundLower.longValue() > boundUpper.longValue()) {
      throw new IllegalArgumentException("Bounds must not be mismatched!");
    }

    this.boundValue = boundValue;
    this.boundUpper = boundUpper;
    this.boundLower = boundLower;
  }

  public static Normal<Double> normalize(Double boundValue) {
    return new Normal<Double>(boundValue, 1D, 0D);
  }

  public static Normal<Long> normalize(Long boundValue) {
    return new Normal<Long>(boundValue, 1L, 0L);
  }

  public static Normal<Integer> normalize(Integer boundValue) {
    return new Normal<Integer>(boundValue, 1, 0);
  }

  public long normalize() {
    long value = boundValue.longValue();
    long lower = boundLower.longValue();
    long upper = boundUpper.longValue();

    return (value - lower) / (upper - lower);
  }

  public long clamp() {
    long value = boundValue.longValue();
    long lower = boundLower.longValue();
    long upper = boundUpper.longValue();

    return Math.max(lower, Math.min(upper, value));
  }

  public long range() {
    return boundUpper.longValue() - boundLower.longValue();
  }

  public boolean isBounded() {
    double value = boundValue.doubleValue();

    return value >= boundLower.doubleValue()
        && value <= boundUpper.doubleValue();
  }

  public boolean isBoundedAbove() {
    return boundValue.doubleValue() > boundUpper.doubleValue();
  }

  public boolean isBoundedBelow() {
    return boundValue.doubleValue() < boundLower.doubleValue();
  }
}