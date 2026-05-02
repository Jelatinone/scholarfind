package com.github.jelatinone.policy.base;

import com.github.jelatinone.model.struct.Document;
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
public final class AttemptsPolicy<Documents extends Document<Documents>, Context extends PolicyContext<Documents>, State>
		implements Policy<Context, State> {
	int maxAttempts;

	@Override
	public PolicyStep<State> apply(Context context, State state) {
		if (context.document().requestHeader().attempt() >= maxAttempts) {
			return new PolicyStep.Decide<>(
					new PolicyDecision.Drop<State>(
							state,
							PolicyReason.ATTEMPTS_EXCEEDED,
							"Request attempts exceeded stage limit"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
