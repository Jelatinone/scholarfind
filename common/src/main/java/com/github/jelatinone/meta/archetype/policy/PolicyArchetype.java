package com.github.jelatinone.meta.archetype.policy;

import java.time.Duration;
import java.time.Instant;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.Retrieve;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyPipeline;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.RetryDirective;

import lombok.NonNull;

/**
 * 
 * <h1>PolicyArchetype</h1>
 * 
 * Pure policy flow contract for building context, state, and a document
 * candidate from a given {@link Letter}.
 * 
 * @author Cody Washington
 * 
 */
public interface PolicyArchetype<In extends Request, Context, State, D extends Document<D>> {

	PolicyPipeline<Context, State> policyPipeline();

	Context buildContext(@NonNull Letter<In> input, @NonNull Instant startedAt);

	State buildState(@NonNull Context context);

	D buildDocument(
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull Instant occurredAt);

	default PolicyDecision<State> failureDecision(@NonNull Letter<In> input, @NonNull Throwable throwable) {
		return PolicyDecision.retry(
				null,
				PolicyReason.OPERATION_EXCEPTION,
				throwable.getMessage(),
				RetryDirective.delay(Duration.ofSeconds(30)));
	}

	default PolicyResult<Letter<In>, Context, State, D> processPolicy(@NonNull Letter<In> input) {
		Instant startedAt = Instant.now();

		Context context = buildContext(input, startedAt);
		State state = buildState(context);
		PolicyDecision<State> decision = policyPipeline().process(context, state);

		Instant occurredAt = Instant.now();
		D document = buildDocument(input, context, decision, occurredAt);

		return new PolicyResult<>(input, context, decision, document, startedAt, occurredAt);
	}

	default PolicyResult<Letter<In>, Context, State, D> recoverPolicy(
			@NonNull Letter<In> input,
			@NonNull Throwable throwable) {
		Instant startedAt = Instant.now();
		PolicyDecision<State> decision = failureDecision(input, throwable);
		Instant occurredAt = Instant.now();

		return new PolicyResult<>(input, null, decision, null, startedAt, occurredAt);
	}

	default Operate<Letter<In>, PolicyResult<Letter<In>, Context, State, D>> policyOperate() {
		return new PolicyOperation<>(this);
	}

	default Retrieve<Letter<In>, PolicyResult<Letter<In>, Context, State, D>> policyRecover() {
		return this::recoverPolicy;
	}
}
