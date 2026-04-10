package com.github.jelatinone.policy;

import java.time.Instant;

import com.github.jelatinone.models.shared.StageDocument;

public final class ExpirationPolicy<Document extends StageDocument<Document>, Context extends PolicyContext<Document>, State>
		implements Policy<Context, State> {
	private final int expirationDays;

	public ExpirationPolicy(int expirationDays) {
		this.expirationDays = expirationDays;
	}

	@Override
	public PolicyStep<State> apply(Context context, State state) {
		Instant discoveredAt = context.document().target().discoveredAt();
		if (discoveredAt == null) {
			return new PolicyStep.Continue<>(state);
		}

		Instant expiresAt = discoveredAt.plusSeconds((long) expirationDays * 24 * 60 * 60);
		if (expiresAt.isBefore(Instant.now())) {
			return new PolicyStep.Decide<>(
					PolicyDecision.drop(state, PolicyReason.DOCUMENT_EXPIRED, "Target expired before stage processing"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
