package com.github.jelatinone.meta.result;

import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.model.struct.Document;

public record PersistResult<State, Documents extends Document<Documents>>(
		Documents document,
		PolicyDecision<State> decision) {
}
