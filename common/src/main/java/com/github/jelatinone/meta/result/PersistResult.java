package com.github.jelatinone.meta.result;

import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.model.struct.Document;

public record PersistResult<State, D extends Document<D>>(
		D document,
		PolicyDecision<State> decision) {
}
