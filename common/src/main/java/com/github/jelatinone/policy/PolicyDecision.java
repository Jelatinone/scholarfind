package com.github.jelatinone.policy;

import java.util.Set;

import com.github.jelatinone.model.audit.AttemptReason;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Emission;

public sealed interface PolicyDecision<State> {

	State state();

	String detail();

	AttemptReason reason();

	record Next<State>(State state, AttemptReason reason, String detail, Set<Emission<? extends Request>> emissions)
			implements PolicyDecision<State> {

		public Next(State state) {
			this(state, null, null, Set.of());
		}
	}

	record Drop<State>(State state, AttemptReason reason, String detail) implements PolicyDecision<State> {
	}

	record Retry<State>(State state, AttemptReason reason, String detail) implements PolicyDecision<State> {
	}

	record Error<State>(State state, AttemptReason reason, String detail) implements PolicyDecision<State> {
	}
}
