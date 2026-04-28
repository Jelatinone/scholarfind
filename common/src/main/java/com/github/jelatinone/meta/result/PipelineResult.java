package com.github.jelatinone.meta.result;

import java.time.Instant;
import java.util.Collection;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;

public record PipelineResult<D extends Document<?>, In extends Request>(
		Letter<In> input,
		D document,
		PolicyDecision<?> decision,
		Collection<Letter<Request>> emissions,
		Instant startedAt,
		Instant occurredAt) {
}
