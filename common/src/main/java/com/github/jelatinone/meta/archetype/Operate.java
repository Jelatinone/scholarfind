package com.github.jelatinone.meta.archetype;

@FunctionalInterface
public interface Operate<Consumes, Produces> {

	Produces operate(Consumes operand);
}
