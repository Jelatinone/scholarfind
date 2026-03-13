package com.github.scholarfind.meta.transitory;

import java.util.List;
import java.util.Set;

import com.github.scholarfind.models.shared.ReasonCode;
import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.StageDocument;

public record Outcome<D extends StageDocument<?>>(
    D document,
    Disposition disposition,
    Set<ReasonCode> reasonCodes,
    List<Emission<? extends Request>> emissions) {
}
