package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import javax.lang.model.element.ModuleElement.Directive;

import lombok.NonNull;

public record AttemptEvent(
    @NonNull UUID requestId,
    @NonNull UUID reviewId,
    @NonNull UUID targetId,

    Set<AttemptReason> reasons,
    @NonNull Directive disposition,

    @NonNull Instant occurredAt,
    @NonNull Instant emittedAt) {
}
