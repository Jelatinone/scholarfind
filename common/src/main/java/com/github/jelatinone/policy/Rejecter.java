package com.github.jelatinone.policy;

@FunctionalInterface
public interface Rejecter<State> {
  PolicyDecision<State> reject(State state, PolicyReason reason, String detail);
}
