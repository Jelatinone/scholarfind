package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.PostResult;

import lombok.NonNull;

/**
 * 
 * <h1>Archetype</h1>
 * 
 * Generic unit of task composition, which collects data, operates on it, and
 * produces a given result.
 * 
 * @author Cody Washington
 */
public interface Archetype<Consumes, Produces> extends AutoCloseable {

	/**
	 * Collects all consumable data into a single collection for
	 * {@link #operate(Object) operation} to be performed on each element
	 * within the collection.
	 * 
	 * @return Collection of consumable data
	 */
	@NonNull
	CollectionResult<@NonNull Consumes> collect();

	/**
	 * Performs an operation on `consumable` data and maps to a `producible` a
	 * result.
	 * 
	 * @param operand Data to be mapped
	 * @return Mapped result
	 */
	Produces operate(final @NonNull Consumes operand);

	/**
	 * Self-callback function to determine the validity of the resulting data
	 * 
	 * @param operand Data to be checked
	 * @return Mapped result
	 */
	@NonNull
	PostResult post(final Produces operand);

	@Override
	default void close() throws Exception {
	}
}
