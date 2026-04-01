package com.github.jelatinone.policy;

public sealed interface PolicyStep<State> permits PolicyStep.Continue, PolicyStep.Decide {

  State state();

  record Continue<State>(State state) implements PolicyStep<State> {
  }

  record Decide<State>(PolicyDecision<State> decision) implements PolicyStep<State> {

    @Override
    public State state() {
      return decision.state();
    }
  }
}
