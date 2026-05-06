package com.github.jelatinone.policy;

import java.util.Set;

import com.github.jelatinone.model.audit.AttemptReason;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Emission;

/**
 * 
 * <h1>PolicyDecision</h1>
 * 
 * Terminal result of a {@link Policy policy} application resulting from a
 * {@link PolicyStep policy step}.
 * 
 * @author Cody Washington
 */
public sealed interface PolicyDecision<State> {

	/**
	 * Terminal state of the policy step
	 * 
	 * @return terminal state
	 */
	State state();

	/**
	 * Explanation for producing a terminal decision
	 * 
	 * @return explanation
	 */
	String detail();

	/**
	 * Reason for producing a terminal decision
	 * 
	 * @return terminal reason
	 */
	AttemptReason reason();

	/**
	 * 
	 * <h1>Next</h1>
	 * 
	 * Policy application has decided terminally that the policy operand may be
	 * emitted.
	 * 
	 */
	record Next<State>(State state, AttemptReason reason, String detail, Set<Emission<? extends Request>> emissions)
			implements PolicyDecision<State> {

		public Next(State state) {
			this(state, null, null, Set.of());
		}
	}

	/**
	 * 
	 * <h1>Drop</h1>
	 * 
	 * Policy application has decided terminally that the policy operand must be
	 * dropped.
	 * 
	 */
	record Drop<State>(State state, AttemptReason reason, String detail) implements PolicyDecision<State> {
	}

	/**
	 * 
	 * <h1>Retry</h1>
	 * 
	 * Policy application has decided that the policy was unsuccessful, and the
	 * operand may be logically retried.
	 * 
	 */
	record Retry<State>(State state, AttemptReason reason, String detail,
			Throwable cause) implements PolicyDecision<State> {
	}

	/**
	 * 
	 * <h1>Error</h1>
	 * 
	 * Policy application has decided that the policy was unsuccessful, and the
	 * operand must not be retired.
	 * 
	 */
	record Error<State>(State state, AttemptReason reason, String detail, Throwable cause)
			implements PolicyDecision<State> {
	}
}
