package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;

import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;

import lombok.NonNull;

/**
 * 
 * <h1>PolicyArchetype</h1>
 * 
 * Pure policy flow contract for building context, state, and a decision from a
 * given {@link Letter}.
 * 
 * @author Cody Washington
 * 
 */
public interface PolicyArchetype<In extends Request, Context, State> {

	PolicyDecision<State> process(Context context, State state);

	State buildState(@NonNull Context context);

	Context buildContext(@NonNull Letter<In> input, @NonNull Instant startedAt);

	default PolicyResult<Letter<In>, Context, State> processPolicy(@NonNull Letter<In> input) {
		Instant startedAt = Instant.now();

		Context context = buildContext(input, startedAt);
		State state = buildState(context);

		PolicyDecision<State> decision = process(context, state);
		Instant occurredAt = Instant.now();

		return new PolicyResult<>(input, context, decision, startedAt, occurredAt);
	}

	default PolicyResult<Letter<In>, Context, State> recoverPolicy(@NonNull Letter<In> input,
			@NonNull Throwable throwable) {
		Instant initializedAt = Instant.now();

		PolicyDecision<State> decision = failureDecision(input, throwable);
		Instant occurredAt = Instant.now();

		return new PolicyResult<>(input, null, decision, initializedAt, occurredAt);
	}

	default PolicyDecision<State> failureDecision(@NonNull Letter<In> input, @NonNull Throwable throwable) {
		return new PolicyDecision.Retry<State>(
				null,
				PolicyReason.OPERATION_EXCEPTION,
				throwable.getMessage());
	}
}
