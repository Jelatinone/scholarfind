package com.github.jelatinone.meta.result;

import com.github.jelatinone.meta.Task;

/**
 * 
 * <h1>PostResult</h1>
 * 
 * Describes a general result from a {@link Task#post()}
 * posting} operation occurring, which may be in one of three states:
 * {@link Success success}, {@link Retry retry}, and {@link Fatal fatal}.
 * 
 * @author Cody Washington
 * 
 */
public sealed interface PostResult {

	/**
	 * Describes a successful result of a persist operation
	 */
	record Success() implements PostResult {
	}

	/**
	 * Describes a failed, but recoverable persist operation
	 */
	record Retry(Throwable throwable) implements PostResult {
	}

	/**
	 * Describes a failed, and unrecoverable persist operation
	 */
	record Fatal(Throwable throwable) implements PostResult {
	}
}
