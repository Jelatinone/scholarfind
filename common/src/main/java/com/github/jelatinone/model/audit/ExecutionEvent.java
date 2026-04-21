package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.policy.StageOutcome;

public record ExecutionEvent(
		UUID targetId,

		ProcessingStage stage,

		Collection<AttemptTransition> transitions,
		StageOutcome outcome,

		Instant emittedAt) {
}
