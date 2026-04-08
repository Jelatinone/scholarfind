package com.github.jelatinone.policy;

import com.github.jelatinone.models.shared.StageDocument;

public final class AttemptsPolicy<D extends StageDocument<D>, C extends PolicyContext<D>, State>
		implements Policy<C, State> {
	private final int maxAttempts;

	public AttemptsPolicy(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	@Override
	public PolicyStep<State> apply(C context, State state) {
		if (context.document().requestHeader().attempt() >= maxAttempts) {
			return new PolicyStep.Decide<>(
					PolicyDecision.error(state, PolicyReason.ATTEMPTS_EXCEEDED, "Request attempts exceeded stage limit"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
