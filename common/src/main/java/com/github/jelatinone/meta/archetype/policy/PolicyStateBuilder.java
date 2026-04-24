package com.github.jelatinone.meta.archetype.policy;

@FunctionalInterface
public interface PolicyStateBuilder<Context, State> {

	State build(Context context);
}
