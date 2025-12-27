package com.github.scholarfind.task.signal;

public sealed interface SignalValue {

	public record NumericSignal(double value) implements SignalValue {
	}

	public record BooleanSignal(boolean value) implements SignalValue {
	}

	public record StringSignal(String value) implements SignalValue {
	}
}
