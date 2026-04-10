package com.github.jelatinone.policy;

import com.github.jelatinone.models.shared.StageDocument;

public final class AttemptsPolicy<Document extends StageDocument<Document>, Context extends PolicyContext<Document>, State>
		implements Policy<Context, State> {
	private final int maxAttempts;

	public AttemptsPolicy(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	@Override
	public PolicyStep<State> apply(Context context, State state) {
		if (context.document().requestHeader().attempt() >= maxAttempts) {
			return new PolicyStep.Decide<>(
					PolicyDecision.error(state, PolicyReason.ATTEMPTS_EXCEEDED, "Request attempts exceeded stage limit"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
