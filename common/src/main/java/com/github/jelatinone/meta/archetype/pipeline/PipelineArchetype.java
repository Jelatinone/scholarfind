package com.github.jelatinone.meta.archetype.pipeline;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.Retrieve;
import com.github.jelatinone.meta.archetype.policy.PolicyArchetype;
import com.github.jelatinone.meta.archetype.policy.PolicyResult;
import com.github.jelatinone.meta.result.PersistResult;
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
public interface PipelineArchetype<In extends Request, Out extends Request, Context, State, Doc extends Document<Doc>> {

  PolicyArchetype<In, Context, State> policy();

  Doc buildDocument(
      @NonNull Letter<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Instant occurredAt);

  PersistResult<State, Doc> persistDocument(
      @NonNull Letter<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Doc document);

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
      @NonNull Emission<Emit> emission,
      @NonNull Letter<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Doc document);

  default Collection<Letter<? extends Request>> buildEmissions(
      @NonNull Letter<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Doc document) {
    return decision.emissions().stream()
        .<Letter<? extends Request>>map(emission -> buildEnvelope(emission, input, context, decision, document))
        .toList();
  }

  default Letter<In> retryEnvelope(
      @NonNull PipelineResult<Doc, In> output) {
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

  default Letter<In> errorEnvelope(@NonNull PipelineResult<Doc, In> output) {
    return output.input();
  }

  default PipelineResult<Doc, In> processPipeline(@NonNull Letter<In> input) {
    PolicyResult<Letter<In>, Context, State> policy = policy().processPolicy(input);
    Doc document = buildDocument(input, policy.context(), policy.decision(), policy.occurredAt());
    PersistResult<State, Doc> persisted = persistDocument(
        input,
        policy.context(),
        policy.decision(),
        document);

    Doc persistedDocument = persisted.document();
    PolicyDecision<State> persistedDecision = persisted.decision();

    persistExecution(input, persistedDecision, policy.occurredAt());
    persistAttempt(input, persistedDecision, policy.startedAt(), policy.occurredAt(), null);

    Collection<Letter<? extends Request>> emissions = persistedDocument == null
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

  default PipelineResult<Doc, In> recoverPipeline(
      @NonNull Letter<In> input,
      @NonNull Throwable throwable) {
    PolicyResult<Letter<In>, Context, State> policy = policy().recoverPolicy(input, throwable);
    PolicyDecision<State> decision = policy.decision();

    persistExecution(input, decision, policy.occurredAt());
    persistAttempt(input, decision, policy.startedAt(), policy.occurredAt(), throwable);

    return new PipelineResult<>(input, null, decision, List.of(), policy.startedAt(), policy.occurredAt());
  }

  default Operate<Letter<In>, PipelineResult<Doc, In>> pipelineOperate() {
    return new PipelineOperation<>(this);
  }

  default Retrieve<Letter<In>, PipelineResult<Doc, In>> pipelineRecover() {
    return this::recoverPipeline;
  }
}
