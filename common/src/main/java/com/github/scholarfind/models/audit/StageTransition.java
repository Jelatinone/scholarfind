package com.github.scholarfind.models.audit;

import java.time.Instant;

import com.github.scholarfind.policy.StageOutcome;

public record StageTransition(
    Instant occurredAt,
    StageOutcome outcome,
    String reasonCode,
    String reasonDetail) {
}
