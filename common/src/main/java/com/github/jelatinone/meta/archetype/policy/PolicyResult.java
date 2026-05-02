package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;

import com.github.jelatinone.policy.PolicyDecision;

public record PolicyResult<Input, Context, State>(
		Input input,
		Context context,

		PolicyDecision<State> decision,

		Instant initializedAt,
		Instant emittedAt) {
}
