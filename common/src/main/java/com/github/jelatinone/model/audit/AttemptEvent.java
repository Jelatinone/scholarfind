package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import javax.lang.model.element.ModuleElement.Directive;

import lombok.NonNull;

public record AttemptEvent(
		@NonNull UUID requestId,
		@NonNull UUID reviewId,
		@NonNull UUID targetId,

		Collection<AttemptReason> reasons,
		@NonNull Directive disposition,

		@NonNull Instant occurredAt,
		@NonNull Instant emittedAt) {
}
