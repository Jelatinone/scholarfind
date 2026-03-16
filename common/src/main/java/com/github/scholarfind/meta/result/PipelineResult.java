package com.github.scholarfind.meta.result;

import java.util.List;

import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.policy.PolicyDecision;

public record PipelineResult<D extends StageDocument<?>, I extends Request, R extends Request>(
    StageEnvelope<I> input,
    D document,
    PolicyDecision<?> decision,
    List<StageEnvelope<R>> emissions) {
}
