package com.github.jelatinone.meta.transitory;

import java.util.List;
import java.util.Set;

import com.github.jelatinone.models.shared.ReasonCode;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.StageDocument;

public record Outcome<D extends StageDocument<?>>(
        D document,
        Disposition disposition,
        Set<ReasonCode> reasonCodes,
        List<Emission<? extends Request>> emissions) {
}
