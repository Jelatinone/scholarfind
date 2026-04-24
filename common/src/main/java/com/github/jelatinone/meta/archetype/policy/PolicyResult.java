package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;

import com.github.jelatinone.policy.PolicyDecision;

public record PolicyResult<Input, Context, State, Document>(
		Input input,
		Context context,
		PolicyDecision<State> decision,
		Document document,
		Instant startedAt,
		Instant occurredAt) {
}
