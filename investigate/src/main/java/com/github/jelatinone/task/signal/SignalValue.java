package com.github.jelatinone.task.signal;

import java.util.List;

public sealed interface SignalValue {

  record NumericSignal(double value) implements SignalValue {
  }

  record BooleanSignal(boolean value) implements SignalValue {
  }

  record StringSignal(String value) implements SignalValue {
  }

  record EnumSignal<Enumerable extends Enum<Enumerable>>(Enumerable value) implements SignalValue {
  }

  record ListSignal<T>(List<T> values) implements SignalValue {
  }
}
