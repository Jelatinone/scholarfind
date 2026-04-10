package com.github.jelatinone.meta.result;

import java.util.Collection;

import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.StageEnvelope;
import com.github.jelatinone.policy.PolicyDecision;

public record PipelineResult<Document extends StageDocument<?>, In extends Request>(
		StageEnvelope<In> input,
		Document document,
		PolicyDecision<?> decision,
		Collection<StageEnvelope<Request>> emissions) {
}
