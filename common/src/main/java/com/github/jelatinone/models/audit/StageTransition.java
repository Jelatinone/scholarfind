package com.github.jelatinone.models.audit;

import java.time.Instant;

import com.github.jelatinone.policy.StageOutcome;

public record StageTransition(
        Instant occurredAt,
        StageOutcome outcome,
        String reasonCode,
        String reasonDetail) {
}
