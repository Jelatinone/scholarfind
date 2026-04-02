package com.github.jelatinone.task.evidence;

import java.util.Optional;
import java.util.function.Function;

import com.github.jelatinone.models.investigate.ClassificationKind;
import com.github.jelatinone.task.signal.SignalIdentifier;
import com.github.jelatinone.task.signal.SignalValue;

public record EvidenceRule(
    SignalIdentifier signal,
    ClassificationKind classification,
    Function<Optional<SignalValue>, Double> rule) {
}
