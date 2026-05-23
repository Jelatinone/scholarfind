package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Set;

import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;
import com.github.jelatinone.policy.PolicyOutcome;

import lombok.NonNull;

public record AttemptEvent(
    @NonNull ReviewIdentity reviewId,
    @NonNull TargetIdentity targetId,

    Set<AttemptReason> reasons,
    @NonNull PolicyOutcome disposition,

    @NonNull Instant occurredAt,
    @NonNull Instant emittedAt) {
}
