package com.github.jelatinone.policy;

/**
 * 
 * <h1>PolicyStep</h1>
 * 
 * Result of a policy which has updated state, which may choose to
 * {@link Continue continue} operation or produce a terminal {@link Decide
 * decision}.
 * 
 * @author Cody Washington
 */
public sealed interface PolicyStep<State> {

	/**
	 * Resulting state of the latest {@link Policy policy} application
	 * 
	 * @return transition state
	 */
	State state();

	/**
	 * <h1>Continue</h1>
	 * 
	 * State transitional operation produced by a {@link Policy policy} pipeline
	 * which forwards the transitioned state to the next policy.
	 * 
	 */
	record Continue<State>(State state) implements PolicyStep<State> {
	}

	/**
	 * <h1>Decide</h1>
	 * 
	 * Terminal step within produced by a {@link Policy policy} pipeline which
	 * decides the outcome of the pipeline.
	 * 
	 */
	record Decide<State>(PolicyDecision<State> decision) implements PolicyStep<State> {

		@Override
		public State state() {
			return decision.state();
		}
	}
}
