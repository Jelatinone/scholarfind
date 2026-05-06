package com.github.jelatinone.policy;

/**
 * <h1>Policy</h1>
 * 
 * Performs a stateful operation given some context, and returns a continuation
 * or terminal resulting step with an updated state.
 */
@FunctionalInterface
public interface Policy<Context, State> {

	/**
	 * Apply this policy on a given context, with the current operational state and
	 * produce a valid forward step.
	 * 
	 * @param context Execution context
	 * @param state   Execution State
	 * @return Forward step
	 * 
	 * @apiNote Policies should be composed within a {@link PolicyPipeline
	 *          pipeline}, and this method should not be called directly
	 */
	PolicyStep<State> apply(Context context, State state);
}
