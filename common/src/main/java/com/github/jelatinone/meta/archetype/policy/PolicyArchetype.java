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

	PolicyDecision<State> pipeline(Context context, State state);

	State buildState(@NonNull Context context);

	Context buildContext(@NonNull Letter<In> input, @NonNull Instant initializedAt);

	default PolicyResult<Letter<In>, Context, State> processPolicy(@NonNull Letter<In> input) {
		Instant initializedAt = Instant.now();

		Context context = buildContext(input, initializedAt);
		State state = buildState(context);

		PolicyDecision<State> decision = pipeline(context, state);
		Instant occurredAt = Instant.now();

		return new PolicyResult<>(input, context, decision, initializedAt, occurredAt);
	}

	default PolicyResult<Letter<In>, Context, State> recoverPolicy(@NonNull Letter<In> input, Throwable throwable) {
		Instant initializedAt = Instant.now();

		PolicyDecision<State> decision = pipelineRecoverable(input, throwable);
		Instant occurredAt = Instant.now();

		return new PolicyResult<>(input, null, decision, initializedAt, occurredAt);
	}

	default PolicyDecision<State> pipelineRecoverable(@NonNull Letter<In> input, Throwable throwable) {
		return new PolicyDecision.Retry<State>(
				null,
				PolicyReason.OPERATION_EXCEPTION,
				throwable.getMessage(),
				throwable);
	}
}
