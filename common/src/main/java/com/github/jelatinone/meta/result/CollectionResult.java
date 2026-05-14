package com.github.jelatinone.meta.result;

import java.util.Collection;
import com.github.jelatinone.meta.Task;

/**
 * 
 * <h1>CollectionResult</h1>
 * 
 * Describes a general collection resulting from a {@link Task#collect()}
 * collection} operation occurring, which may be in one of two states:
 * {@link Alive alive} and {@link Empty empty}.
 * 
 * @author Cody Washington
 */
public sealed interface CollectionResult<Consumes> {

	/**
	 * Describes a list of data with elements
	 */
	record Alive<Consumes>(Collection<Consumes> collection) implements CollectionResult<Consumes> {
	}

	/**
	 * Describes a collection source without currently available elements.
	 */
	record Empty<Consumes>() implements CollectionResult<Consumes> {
	}
}
