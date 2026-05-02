package com.github.jelatinone.meta.archetype.policy;

import com.github.jelatinone.meta.archetype.Persist;
import com.github.jelatinone.meta.result.PostResult;
import com.github.jelatinone.policy.PolicyDecision;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyPersist<Input, Context, State> implements Persist<PolicyResult<Input, Context, State>> {

	PolicyDisposition<Input, Context, State> disposition;

	@Override
	public PostResult post(PolicyResult<Input, Context, State> operand) {
		if (operand == null || operand.decision() == null) {
			return PostResult.FAILURE_FATAL;
		}
		try {
			switch (operand.decision()) {
				case PolicyDecision.Next<State> ignored ->
					disposition.next(operand);
				case PolicyDecision.Drop<State> ignored ->
					disposition.drop(operand);
				case PolicyDecision.Retry<State> ignored ->
					disposition.retry(operand);
				case PolicyDecision.Error<State> ignored ->
					disposition.error(operand);
			}
			return PostResult.SUCCESS;
		} catch (Exception exception) {
			return PostResult.FAILURE_RETRY;
		}
	}
}
