package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;

import com.github.jelatinone.policy.PolicyDecision;

@FunctionalInterface
public interface PolicyDocumentBuilder<Input, Context, State, Document> {

	Document build(
			Input input,
			Context context,
			PolicyDecision<State> decision,
			Instant occurredAt);
}
