package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.OperationResult;
import com.github.jelatinone.meta.result.PostResult;

import lombok.NonNull;

public record Composition<Consumes, Produces>(
		Collect<Consumes> source,
		Operate<Consumes, Produces> operation,
		Persist<Produces> persist) implements Archetype<Consumes, Produces> {

	@Override
	public @NonNull CollectionResult<@NonNull Consumes> collect() {
		return source().collect();
	}

	@Override
	public OperationResult<Produces> operate(@NonNull Consumes operand) {
		return operation().operate(operand);
	}

	@Override
	public @NonNull PostResult post(OperationResult<Produces> operand) {
		return persist().post(operand);
	}

}
