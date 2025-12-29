package com.github.scholarfind.task.signal;

import java.util.List;

public sealed interface SignalValue {

	public record NumericSignal(double value) implements SignalValue {
	}

	public record BooleanSignal(boolean value) implements SignalValue {
	}

	public record StringSignal(String value) implements SignalValue {
	}

	public record ListSignal<T>(List<T> values) implements SignalValue {

	}
}
