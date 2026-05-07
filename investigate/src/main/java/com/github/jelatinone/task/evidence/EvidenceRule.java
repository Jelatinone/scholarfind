package com.github.jelatinone.task.evidence;

import java.util.Optional;
import java.util.function.Function;

import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalValue;

public record EvidenceRule(
    SignalIdentity identity,
    Category classification,
    Function<Optional<SignalValue>, Double> rule) {
}
