package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.CollectionResult;

import lombok.NonNull;

@FunctionalInterface
public interface Collect<Consumes> extends AutoCloseable {

	/**
	 * Collects all consumable data into a single collection for
	 * {@link #operate(Object) operation} to be performed on each element
	 * within the collection.
	 * 
	 * @return Collection of consumable data
	 */
	@NonNull
	CollectionResult<Consumes> collect();

	@Override
	default void close() throws Exception {
	}
}
