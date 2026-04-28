package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.Retrieve;
import com.github.jelatinone.meta.result.PersistResult;
import com.github.jelatinone.meta.result.PipelineResult;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.EmissionIntent;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.RetryDirective;

import lombok.NonNull;

/**
 * 
 * <h1>PipelineArchetype</h1>
 * 
 * Stage lifecycle boilerplate layered on top of {@link PolicyArchetype}
 * without assuming any queue transport behavior.
 * 
 * @author Cody Washington
 * 
 */
public interface PipelineArchetype<In extends Request, Out extends Request, Context, State, D extends Document<D>>
		extends PolicyArchetype<In, Context, State, D> {

	PersistResult<State, D> persistDocument(
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull D document);

	void persistExecution(
			@NonNull Letter<In> input,
			@NonNull PolicyDecision<?> decision,
			@NonNull Instant occurredAt);

	void persistAttempt(
			@NonNull Letter<In> input,
			@NonNull PolicyDecision<?> decision,
			@NonNull Instant startedAt,
			@NonNull Instant occurredAt,
			Throwable throwable);

	In buildRequest(@NonNull In request, @NonNull RequestHeader header);

	<Emit extends Request> Letter<Emit> buildEnvelope(
			@NonNull EmissionIntent<? extends Request> emission,
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull D document);

	default Collection<Letter<Request>> buildEmissions(
			@NonNull Letter<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull D document) {
		return decision.emissionIntents().stream()
				.map(emission -> buildEnvelope(emission, input, context, decision, document))
				.toList();
	}

	default Letter<In> retryEnvelope(
			@NonNull PipelineResult<D, In> output,
			@NonNull RetryDirective retryDirective) {
		In payload = output.input().content();

		RequestHeader nextHeader = RequestHeader.retry(payload.requestHeader(), Instant.now());
		In nextRequest = buildRequest(payload, nextHeader);
		return new Letter<>(
				output.input().schemaVersion(),
				output.input().targetId(),
				output.input().reviewId(),
				output.input().executionRef(),
				nextRequest,
				Instant.now());
	}

	default Letter<In> errorEnvelope(@NonNull PipelineResult<D, In> output) {
		return output.input();
	}

	default PipelineResult<D, In> processPipeline(@NonNull Letter<In> input) {
		PolicyResult<Letter<In>, Context, State, D> policy = processPolicy(input);
		PersistResult<State, D> persisted = persistDocument(
				input,
				policy.context(),
				policy.decision(),
				policy.document());

		D persistedDocument = persisted.document();
		PolicyDecision<State> persistedDecision = persisted.decision();

		persistExecution(input, persistedDecision, policy.occurredAt());
		persistAttempt(input, persistedDecision, policy.startedAt(), policy.occurredAt(), null);

		Collection<Letter<Request>> emissions = persistedDocument == null
				? List.of()
				: buildEmissions(input, policy.context(), persistedDecision, persistedDocument);
		return new PipelineResult<>(
				input,
				persistedDocument,
				persistedDecision,
				emissions,
				policy.startedAt(),
				policy.occurredAt());
	}

	default PipelineResult<D, In> recoverPipeline(
			@NonNull Letter<In> input,
			@NonNull Throwable throwable) {
		PolicyResult<Letter<In>, Context, State, D> policy = recoverPolicy(input, throwable);
		PolicyDecision<State> decision = policy.decision();

		persistExecution(input, decision, policy.occurredAt());
		persistAttempt(input, decision, policy.startedAt(), policy.occurredAt(), throwable);

		return new PipelineResult<>(input, null, decision, List.of(), policy.startedAt(), policy.occurredAt());
	}

	default Operate<Letter<In>, PipelineResult<D, In>> pipelineOperate() {
		return this::processPipeline;
	}

	default Retrieve<Letter<In>, PipelineResult<D, In>> pipelineRecover() {
		return this::recoverPipeline;
	}
}
