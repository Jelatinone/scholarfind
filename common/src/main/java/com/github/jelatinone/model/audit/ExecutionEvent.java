package com.github.jelatinone.model.audit;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.github.jelatinone.policy.StageOutcome;

import lombok.NonNull;

public record ExecutionEvent(
		@NonNull UUID targetId,

		@NonNull ExecutionStage executionRef,

		Collection<AttemptTransition> transitions,
		@NonNull StageOutcome outcome,

		@NonNull Instant attemptAt,
		@NonNull Instant emittedAt) {
}
