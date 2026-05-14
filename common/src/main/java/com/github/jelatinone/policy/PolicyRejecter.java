package com.github.jelatinone.policy;

@FunctionalInterface
public interface PolicyRejecter<State> {

  PolicyDecision<State> reject(State state, PolicyReason reason, String detail);
}
