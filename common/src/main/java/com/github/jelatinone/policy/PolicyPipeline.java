package com.github.jelatinone.policy;

import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * 
 * <h1>PolicyPipeline</h1>
 * 
 * Operable unit of a set of ordered policies which produce a terminal policy
 * decision.
 * 
 * @author Cody Washington
 * 
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyPipeline<Context, State> {
	List<Policy<Context, State>> policies;

	/**
	 * Constructor.
	 * 
	 * @param policies Ordered list of operable policies, assigned in the order of
	 *                 pipeline execution
	 */
	public PolicyPipeline(List<Policy<Context, State>> policies) {
		this.policies = List.copyOf(policies);
	}

	/**
	 * Process this pipeline and produce a terminal decision based on initial
	 * context and state
	 * 
	 * @param context policy context
	 * @param state   initial state
	 * @return terminal policy decision
	 */
	public PolicyDecision<State> process(final Context context, State state) {
		State current = state;
		for (Policy<Context, State> policy : policies) {
			PolicyStep<State> step = policy.apply(context, current);
			switch (step) {
				case PolicyStep.Continue<State>(State next) -> current = next;
				case PolicyStep.Decide<State>(PolicyDecision<State> decision) -> {
					return decision;
				}
			}
		}
		return new PolicyDecision.Next<State>(current);
	}
}
