package com.github.scholarfind.task.evidence;

import java.util.Optional;
import java.util.function.Function;

import com.github.scholarfind.models.investigate.ClassificationType;
import com.github.scholarfind.task.signal.SignalIdentifier;
import com.github.scholarfind.task.signal.SignalValue;

public record EvidenceRule(
		SignalIdentifier signal,
		ClassificationType classification,
		Function<Optional<SignalValue>, Double> rule) {
}
