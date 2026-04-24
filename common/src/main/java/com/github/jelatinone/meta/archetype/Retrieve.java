package com.github.jelatinone.meta.archetype;

@FunctionalInterface
public interface Retrieve<Input, Output> {

	Output recover(Input input, Throwable throwable);
}
