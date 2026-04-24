package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.OperationResult;
import com.github.jelatinone.meta.result.PostResult;

@FunctionalInterface
public interface Persist<Produces> {

	PostResult post(OperationResult<Produces> operand);
}
