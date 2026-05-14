package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.policy.PolicyOutcome;

import lombok.NonNull;

public record AttemptEvent(
    @NonNull UUID requestId,
    @NonNull UUID reviewId,
    @NonNull UUID targetId,

    Set<AttemptReason> reasons,
    @NonNull PolicyOutcome disposition,

    @NonNull Instant occurredAt,
    @NonNull Instant emittedAt) {
}
