package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.github.jelatinone.meta.transitory.Disposition;

import lombok.NonNull;

public record AttemptEvent(
		@NonNull UUID requestId,
		@NonNull UUID reviewId,
		@NonNull UUID targetId,

		Collection<AttemptReason> reasons,
		@NonNull Disposition disposition,

		@NonNull Instant occurredAt,
		@NonNull Instant emittedAt) {
}
