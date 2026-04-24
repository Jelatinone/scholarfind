package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.CollectionResult;

@FunctionalInterface
public interface Collect<Consumes> extends AutoCloseable {

	CollectionResult<Consumes> collect();

	@Override
	default void close() throws Exception {
	}
}
