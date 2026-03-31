package com.github.scholarfind.policy;

@FunctionalInterface
public interface Rejecter<State> {
  PolicyDecision<State> reject(State state, PolicyReason reason, String detail);
}
