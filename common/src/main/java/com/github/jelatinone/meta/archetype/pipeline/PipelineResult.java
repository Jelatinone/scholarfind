package com.github.jelatinone.meta.archetype.pipeline;

import java.time.Instant;
import java.util.Collection;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;

public record PipelineResult<Doc extends Document<?>, In extends Request>(
    Letter<In> input,
    Doc document,
    PolicyDecision<?> decision,
    Collection<Letter<? extends Request>> emissions,
    Instant startedAt,
    Instant occurredAt) {
}
