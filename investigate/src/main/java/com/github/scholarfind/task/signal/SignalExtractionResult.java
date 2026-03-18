package com.github.scholarfind.task.signal;

import java.util.Optional;

public sealed interface SignalExtractionResult {

	public static record Both(SignalCost cost, Optional<SignalValue> value) implements SignalExtractionResult {
	}

	public static record Value(Optional<SignalValue> value) implements SignalExtractionResult {
	}

	public static record Cost(SignalCost cost) implements SignalExtractionResult {
	}
}
