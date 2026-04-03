package com.github.jelatinone.meta.result;

import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.policy.PolicyDecision;

public record PersistResult<State, Document extends StageDocument<?>>(
		Document document,
		PolicyDecision<State> decision) {
}
