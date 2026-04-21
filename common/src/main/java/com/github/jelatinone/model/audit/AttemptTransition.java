package com.github.jelatinone.model.audit;

import java.time.Instant;

import com.github.jelatinone.policy.StageOutcome;

public record AttemptTransition(
		StageOutcome outcome,
		AttemptReason reason,

		Instant emittedAt) {
}
