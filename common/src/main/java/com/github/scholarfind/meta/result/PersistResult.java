package com.github.scholarfind.meta.result;

import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.policy.PolicyDecision;

public record PersistResult<State, D extends StageDocument<?>>(D document, PolicyDecision<State> decision) {
}
