package com.github.jelatinone.meta.archetype.pipeline;

import java.time.Instant;
import java.util.Collection;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.policy.PolicyDecision;

public record PipelineResult<Documents extends Document<?>, In extends Request<In>>(
		In input,
		Documents document,

		PolicyDecision<?> decision,
		Collection<? extends Request<?>> emissions,

		Instant initializedAt,
		Instant occurredAt) {
}
