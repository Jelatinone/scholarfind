package com.github.jelatinone.models.audit;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.meta.transitory.Disposition;
import com.github.jelatinone.models.shared.ReasonCode;

public record AttemptEvent(
        UUID requestId,
        ProcessingStage stage,
        UUID targetId,
        int attempt,
        Disposition disposition,
        Set<ReasonCode> reasonCodes,
        String errorFingerprint,
        Duration processingTime,
        Instant occurredAt,
        String normalizedUrl,
        int depth,
        String idempotencyKey) {
}
