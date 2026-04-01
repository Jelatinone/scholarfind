package com.github.jelatinone.meta.result;

import java.util.List;

import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.StageEnvelope;
import com.github.jelatinone.policy.PolicyDecision;

public record PipelineResult<D extends StageDocument<?>, I extends Request, R extends Request>(
        StageEnvelope<I> input,
        D document,
        PolicyDecision<?> decision,
        List<StageEnvelope<R>> emissions) {
}
