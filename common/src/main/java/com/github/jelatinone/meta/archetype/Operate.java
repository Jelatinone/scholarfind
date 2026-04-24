package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.OperationResult;

@FunctionalInterface
public interface Operate<Consumes, Produces> {

	OperationResult<Produces> operate(Consumes operand);
}
