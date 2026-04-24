package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;

@FunctionalInterface
public interface PolicyContextBuilder<Input, Context> {

	Context build(Input input, Instant startedAt);
}
