package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.github.jelatinone.policy.StageOutcome;

public record ExecutionEvent(
		UUID targetId,

		ExecutionStage executionRef,

		Collection<AttemptTransition> transitions,
		StageOutcome outcome,

		Instant attemptAt,
		Instant emittedAt) {
}
