package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.github.jelatinone.meta.transitory.Disposition;

public record AttemptEvent(
		UUID requestId,
		UUID reviewId,
		UUID targetId,

		Collection<AttemptReason> reasons,
		Disposition disposition,

		Instant occurredAt,
		Instant emittedAt) {
}
