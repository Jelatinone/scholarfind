package com.github.jelatinone.policy;

@FunctionalInterface
public interface Policy<Context, State> {

  PolicyStep<State> apply(Context context, State state);
}
