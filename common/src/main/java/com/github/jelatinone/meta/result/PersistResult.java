package com.github.jelatinone.meta.result;

import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.policy.PolicyDecision;

public record PersistResult<State, D extends StageDocument<?>>(D document, PolicyDecision<State> decision) {
}
