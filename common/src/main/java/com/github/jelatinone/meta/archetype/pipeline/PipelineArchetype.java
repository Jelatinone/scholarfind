package com.github.jelatinone.meta.archetype.pipeline;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.github.jelatinone.meta.archetype.policy.PolicyArchetype;
import com.github.jelatinone.meta.archetype.policy.PolicyResult;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;

import lombok.NonNull;

/**
 * 
 * <h1>PipelineArchetype</h1>
 * 
 * Stage lifecycle boilerplate layered around a {@link PolicyArchetype}
 * without assuming any queue transport behavior.
 * 
 * @author Cody Washington
 * 
 */
public interface PipelineArchetype<In extends Request, Out extends Request, Context, State, Documents extends Document<Documents>> {

	PersistResult<State, Documents> persistDocument(
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision.Next<State> decision,
			@NonNull Instant occurredAt);

	void persistExecution(
			@NonNull Letter<In> input,
			@NonNull PolicyDecision<?> decision,
			@NonNull Instant occurredAt);

	void persistAttempt(
			@NonNull Letter<In> input,
			@NonNull PolicyDecision<?> decision,
			@NonNull Instant initializedAt,
			@NonNull Instant occurredAt,
			Throwable throwable);

	In buildRequest(@NonNull In request, @NonNull RequestHeader header);

	<Emit extends Request> Letter<Emit> buildEnvelope(
			@NonNull Emission<Emit> emission,
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull Documents document);

	default Collection<Letter<? extends Request>> buildEmissions(
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull Set<Emission<? extends Request>> emissions,
			@NonNull Documents document) {
		return emissions.stream()
				.<Letter<? extends Request>>map(emission -> buildEnvelope(emission, input, context, document))
				.toList();
	}

	default Letter<In> retryEnvelope(
			@NonNull PipelineResult<Documents, In> output) {
		In payload = output.input().content();

		Instant occurredAt = Instant.now();
		RequestHeader nextHeader = RequestHeader.retry(payload.requestHeader(), occurredAt);
		In nextRequest = buildRequest(payload, nextHeader);
		return new Letter<>(
				output.input().schemaVersion(),
				output.input().targetId(),
				output.input().reviewId(),
				output.input().executionRef(),
				nextRequest,
				occurredAt);
	}

	default Letter<In> errorEnvelope(@NonNull PipelineResult<Documents, In> output) {
		return output.input();
	}

	private PipelineResult<Documents, In> processNext(
			Letter<In> input,
			Context context,
			PolicyDecision.Next<State> decision,
			PolicyResult<Letter<In>, Context, State> policy) {

		PersistResult<State, Documents> persisted = persistDocument(
				input,
				context,
				decision,
				policy.emittedAt());

		Documents persistedDocument = persisted.document();
		PolicyDecision<State> persistedDecision = persisted.decision();

		persistExecution(input, persistedDecision, policy.emittedAt());
		persistAttempt(input, persistedDecision, policy.initializedAt(), policy.emittedAt(), null);

		Collection<Letter<? extends Request>> emissions = persistedDocument == null
				? List.of()
				: buildEmissions(input, context, decision.emissions(), persistedDocument);

		return new PipelineResult<>(
				input,
				persistedDocument,
				persistedDecision,
				emissions,
				policy.initializedAt(),
				policy.emittedAt());
	}

	private PipelineResult<Documents, In> processTerminal(
			Letter<In> input,
			PolicyDecision<State> decision,
			PolicyResult<Letter<In>, Context, State> policy) {

		persistExecution(input, decision, policy.emittedAt());
		persistAttempt(input, decision, policy.initializedAt(), policy.emittedAt(), cause(decision));

		return new PipelineResult<>(
				input,
				null,
				decision,
				List.of(),
				policy.initializedAt(),
				policy.emittedAt());
	}

	private Throwable cause(PolicyDecision<State> decision) {
		return switch (decision) {
			case PolicyDecision.Retry<State> retry -> retry.cause();
			case PolicyDecision.Error<State> error -> error.cause();
			default -> null;
		};
	}

	default PipelineResult<Documents, In> processPipeline(@NonNull PolicyResult<Letter<In>, Context, State> policy) {
		Letter<In> input = policy.input();
		PolicyDecision<State> decision = policy.decision();

		return switch (decision) {
			case PolicyDecision.Next<State> next -> processNext(input, policy.context(), next, policy);
			case PolicyDecision.Drop<State> drop -> processTerminal(input, drop, policy);
			case PolicyDecision.Retry<State> retry -> processTerminal(input, retry, policy);
			case PolicyDecision.Error<State> error -> processTerminal(input, error, policy);
		};
	}
}
