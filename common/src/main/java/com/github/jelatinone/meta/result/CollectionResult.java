package com.github.jelatinone.meta.result;

import java.util.Collection;
import com.github.jelatinone.meta.Task;

/**
 * 
 * <h1>CollectionResult</h1>
 * 
 * Describes a general collection resulting from a {@link Task#collect()}
 * collection} operation occurring, which may be in one of three states:
 * {@link Alive alive}, {@link Idle idle}, and {@link Empty empty}.
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
	 * Describes a list of data without elements, and not expecting to receive
	 * future elements within the lifetime of this task
	 */
	record Empty<Consumes>() implements CollectionResult<Consumes> {
	}
}
