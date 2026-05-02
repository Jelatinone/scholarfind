package com.github.jelatinone.meta.archetype;

import lombok.NonNull;

@FunctionalInterface
public interface Retrieve<Consumes, Produces> {

	/**
	 * Performs an operation on `consumable` data and maps to a `producible` a
	 * result and guarantees the existence of a valid result
	 * 
	 * @param operand Data to be mapped
	 * @return Mapped result
	 */
	@NonNull
	Produces recover(Consumes input, Throwable throwable);
}
