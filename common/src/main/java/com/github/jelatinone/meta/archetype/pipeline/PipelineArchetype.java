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
public interface PipelineArchetype<In extends Request<In>, Out extends Request<Out>, Context, State, Documents extends Document<Documents>> {

	PersistResult<State, Documents> persistDocument(
			@NonNull In input,
			@NonNull Context context,
			@NonNull PolicyDecision.Next<State> decision,
			@NonNull Instant occurredAt);

	void persistExecution(
			@NonNull In input,
			@NonNull PolicyDecision<?> decision,
			@NonNull Instant occurredAt);

	void persistAttempt(
			@NonNull In input,
			@NonNull PolicyDecision<?> decision,
			@NonNull Instant initializedAt,
			@NonNull Instant occurredAt,
			Throwable throwable);

	In buildRequest(@NonNull In request, @NonNull RequestHeader header);

	<Emit extends Request<Emit>> Emit buildEnvelope(
			@NonNull Emission<Emit> emission,
			@NonNull In input,
			@NonNull Context context,
			@NonNull Documents document);

	default Collection<? extends Request<?>> buildEmissions(
			@NonNull In input,
			@NonNull Context context,
			@NonNull Set<Emission<? extends Request<?>>> emissions,
			@NonNull Documents document) {
		return emissions.stream()
				.map(emission -> buildEnvelope(emission, input, context, document))
				.toList();
	}

	default In retryEnvelope(@NonNull PipelineResult<Documents, In> output) {
		In payload = output.input();
		Instant occurredAt = Instant.now();

		RequestHeader nextHeader = RequestHeader.retry(payload.requestHeader(), occurredAt);
		In nextRequest = buildRequest(payload, nextHeader);

		return nextRequest;
	}

	default In errorEnvelope(@NonNull PipelineResult<Documents, In> output) {
		return output.input();
	}

	private PipelineResult<Documents, In> processNext(
			In input,
			Context context,
			PolicyDecision.Next<State> decision,
			PolicyResult<In, Context, State> policy) {

		PersistResult<State, Documents> persisted = persistDocument(
				input,
				context,
				decision,
				policy.emittedAt());

		Documents persistedDocument = persisted.document();
		PolicyDecision<State> persistedDecision = persisted.decision();

		persistExecution(input, persistedDecision, policy.emittedAt());
		persistAttempt(input, persistedDecision, policy.initializedAt(), policy.emittedAt(), null);

		Collection<? extends Request<?>> emissions = persistedDocument == null
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
			In input,
			PolicyDecision<State> decision,
			PolicyResult<In, Context, State> policy) {

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

	default PipelineResult<Documents, In> processPipeline(@NonNull PolicyResult<In, Context, State> policy) {
		In input = policy.input();
		PolicyDecision<State> decision = policy.decision();

		return switch (decision) {
			case PolicyDecision.Next<State> next -> processNext(input, policy.context(), next, policy);
			case PolicyDecision.Drop<State> drop -> processTerminal(input, drop, policy);
			case PolicyDecision.Retry<State> retry -> processTerminal(input, retry, policy);
			case PolicyDecision.Error<State> error -> processTerminal(input, error, policy);
		};
	}
}
