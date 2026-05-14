package com.github.jelatinone.model.audit;

import java.time.Instant;

import com.github.jelatinone.policy.PolicyOutcome;

import lombok.NonNull;

public record AttemptTransition(
    @NonNull PolicyOutcome outcome,
    @NonNull AttemptReason reason,

    @NonNull Instant emittedAt) {
}
