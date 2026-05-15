package com.github.jelatinone.policy.base;

import java.time.Instant;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyContext;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ExpirationPolicy<Requests extends Request<Requests>, Documents extends Document<Documents>, Context extends PolicyContext<Requests, Documents>, State>
		implements Policy<Context, State> {
	int expirationDays;

	@Override
	public PolicyStep<State> apply(Context context, State state) {
		Instant discoveredAt = context.retrievedDocument().documentHeader().emittedAt();
		if (discoveredAt == null) {
			return new PolicyStep.Continue<>(state);
		}

		Instant expiresAt = discoveredAt.plusSeconds((long) expirationDays * 24 * 60 * 60);
		if (expiresAt.isBefore(context.envelopeReviewedAt())) {
			return new PolicyStep.Decide<>(
					new PolicyDecision.Drop<State>(
							state,
							PolicyReason.DOCUMENT_EXPIRED,
							"Target expired before stage processing"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
