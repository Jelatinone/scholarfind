package com.github.jelatinone.meta.archetype.policy;

import com.github.jelatinone.meta.archetype.Persist;
import com.github.jelatinone.meta.result.PostResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyPersist<Input, Context, State>
		implements Persist<PolicyResult<Input, Context, State>> {

	PolicyDisposition<Input, Context, State> disposition;

	@Override
	public PostResult post(PolicyResult<Input, Context, State> operand) {
		if (operand == null || operand.decision() == null) {
			return PostResult.FAILURE_FATAL;
		}

		PolicyResult<Input, Context, State> result = operand;
		try {
			switch (result.decision().outcome()) {
				case NEXT ->
					disposition.next(result);
				case DROP ->
					disposition.drop(result);
				case RETRY ->
					disposition.retry(result);
				case ERROR ->
					disposition.error(result);
			}
			return PostResult.SUCCESS;
		} catch (Exception exception) {
			return PostResult.FAILURE_RETRY;
		}
	}
}
