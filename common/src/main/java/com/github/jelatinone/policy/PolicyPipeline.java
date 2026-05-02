package com.github.jelatinone.policy;

import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyPipeline<Context, State> {
	List<Policy<Context, State>> policies;

	public PolicyPipeline(List<Policy<Context, State>> policies) {
		this.policies = List.copyOf(policies);
	}

	public PolicyDecision<State> process(Context context, State state) {
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
