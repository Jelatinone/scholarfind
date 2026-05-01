package com.github.jelatinone.model.audit;

import java.time.Instant;

import com.github.jelatinone.policy.StageOutcome;

import lombok.NonNull;

public record AttemptTransition(
		@NonNull StageOutcome outcome,
		@NonNull AttemptReason reason,

		@NonNull Instant emittedAt) {
}
