package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Set;

import com.github.jelatinone.model.struct.Identity.TargetIdentity;
import com.github.jelatinone.policy.PolicyOutcome;

import lombok.NonNull;

public record ExecutionEvent(
    @NonNull TargetIdentity targetId,

    @NonNull ExecutionStage executionRef,

    Set<AttemptTransition> transitions,
    @NonNull PolicyOutcome outcome,

    @NonNull Instant occurredAt,
    @NonNull Instant emittedAt) {
}
