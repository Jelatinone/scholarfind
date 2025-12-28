package com.github.scholarfind.task.signal;

import java.util.Optional;

import lombok.NonNull;

public sealed interface SignalExtractionResult {

	public static record Both(SignalCost cost, @NonNull Optional<SignalValue> value) implements SignalExtractionResult {
	}

	public static record Value(@NonNull Optional<SignalValue> value) implements SignalExtractionResult {
	}

	public static record Cost(SignalCost cost) implements SignalExtractionResult {
	}
}
