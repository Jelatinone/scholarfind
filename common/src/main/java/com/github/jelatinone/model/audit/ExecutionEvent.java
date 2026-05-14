package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.policy.PolicyOutcome;

import lombok.NonNull;

public record ExecutionEvent(
    @NonNull UUID targetId,

    @NonNull ExecutionStage executionRef,

    Set<AttemptTransition> transitions,
    @NonNull PolicyOutcome outcome,

    @NonNull Instant occurredAt,
    @NonNull Instant emittedAt) {
}
