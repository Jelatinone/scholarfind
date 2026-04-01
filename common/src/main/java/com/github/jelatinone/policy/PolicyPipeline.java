package com.github.jelatinone.policy;

import java.util.List;

public final class PolicyPipeline<Context, State> {
  private final List<Policy<Context, State>> policies;

  public PolicyPipeline(List<Policy<Context, State>> policies) {
    this.policies = List.copyOf(policies);
  }

  public List<Policy<Context, State>> policies() {
    return policies;
  }

  public PolicyDecision<State> process(Context context, State state) {
    State current = state;
    for (Policy<Context, State> policy : policies) {
      PolicyStep<State> step = policy.apply(context, current);
      switch (step) {
        case PolicyStep.Continue<State>(State nextState) -> current = nextState;
        case PolicyStep.Decide<State>(PolicyDecision<State> decision) -> {
          return decision;
        }
      }
    }
    return PolicyDecision.next(current);
  }
}
