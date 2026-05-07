package com.github.jelatinone.task;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import com.github.jelatinone.meta.archetype.pipeline.PersistResult;
import com.github.jelatinone.meta.archetype.pipeline.PipelineArchetype;
import com.github.jelatinone.meta.archetype.pipeline.PipelineResult;
import com.github.jelatinone.meta.archetype.policy.PolicyArchetype;
import com.github.jelatinone.model.annotate.AnnotateRequest;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyPipeline;
import com.github.jelatinone.policy.StageOutcome;
import com.github.jelatinone.task.policy.ClassificationConfiguration;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class InvestigateTask implements
    PolicyArchetype<InvestigateRequest, InvestigateContext, InvestigateState>,
    PipelineArchetype<InvestigateRequest, AnnotateRequest, InvestigateContext, InvestigateState, InvestigateDocument> {

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @NonNull
    PolicyPipeline<InvestigateContext, InvestigateState> policyPipeline;

    @NonNull
    InvestigateInfrastructure infrastructure;

    @NonNull
    ClassificationConfiguration classificationConfiguration;
  }

  Configuration config;

  public InvestigateTask(@NonNull Configuration config) {
    this.config = config;
  }

  public PipelineResult<InvestigateDocument, InvestigateRequest> process(@NonNull Letter<InvestigateRequest> input) {
    return processPipeline(processPolicy(input));
  }

  @Override
  public PolicyDecision<InvestigateState> process(InvestigateContext context, InvestigateState state) {
    return config.policyPipeline.process(context, state);
  }

  @Override
  public InvestigateState buildState(@NonNull InvestigateContext context) {
    return InvestigateState.initial(context.reviewedAt());
  }

  @Override
  public InvestigateContext buildContext(@NonNull Letter<InvestigateRequest> input, @NonNull Instant startedAt) {
    InvestigateRequest request = input.content();
    return new InvestigateContext(
        new InvestigateDocument(
            new DocumentHeader(
                InvestigateDocument.SCHEMA_VERSION,
                input.targetId(),
                input.reviewId(),
                input.executionRef(),
                startedAt),
            request.requestHeader(),
            input.targetId(),
            input.reviewId(),
            emptyClassification(),
            Set.of()),
        request.target(),
        config.classificationConfiguration,
        config.infrastructure.acquisitionService(),
        startedAt,
        config.infrastructure.investigateStore().get(input.targetId()),
        config.infrastructure.contentStore().get(input.targetId()),
        input.schemaVersion(),
        input.executionRef(),
        input.targetId(),
        input.reviewId());
  }

  @Override
  public PersistResult<InvestigateState, InvestigateDocument> persistDocument(
      @NonNull Letter<InvestigateRequest> input,
      @NonNull InvestigateContext context,
      @NonNull PolicyDecision.Next<InvestigateState> decision,
      @NonNull Instant occurredAt) {
    Classification classification = decision.state() == null
        ? context.document().classification()
        : decision.state().classification();
    InvestigateDocument document = new InvestigateDocument(
        new DocumentHeader(
            InvestigateDocument.SCHEMA_VERSION,
            input.targetId(),
            input.reviewId(),
            input.executionRef(),
            occurredAt),
        input.content().requestHeader(),
        input.targetId(),
        input.reviewId(),
        classification,
        Set.of());
    config.infrastructure.investigateStore().put(document);
    return new PersistResult<>(document, decision);
  }

  @Override
  public void persistExecution(
      @NonNull Letter<InvestigateRequest> input,
      @NonNull PolicyDecision<?> decision,
      @NonNull Instant occurredAt) {
    config.infrastructure.executionStore().put(new ExecutionEvent(
        input.targetId(),
        input.executionRef(),
        Set.of(),
        outcome(decision),
        occurredAt,
        Instant.now()));
  }

  @Override
  public void persistAttempt(
      @NonNull Letter<InvestigateRequest> input,
      @NonNull PolicyDecision<?> decision,
      @NonNull Instant initializedAt,
      @NonNull Instant occurredAt,
      Throwable throwable) {
    // Attempt audit is intentionally left untouched until common.model.audit is rewritten.
  }

  @Override
  public InvestigateRequest buildRequest(@NonNull InvestigateRequest request, @NonNull RequestHeader header) {
    return new InvestigateRequest(header, request.target());
  }

  @Override
  public <Emit extends Request> Letter<Emit> buildEnvelope(
      @NonNull Emission<Emit> emission,
      @NonNull Letter<InvestigateRequest> input,
      @NonNull InvestigateContext context,
      @NonNull InvestigateDocument document) {
    Emit request = emission.request();
    return new Letter<>(
        request.targetId(),
        request.reviewId(),
        emission.executionRef(),
        request,
        Instant.now());
  }

  @Override
  public Collection<Letter<? extends Request>> buildEmissions(
      @NonNull Letter<InvestigateRequest> input,
      @NonNull InvestigateContext context,
      @NonNull java.util.Set<Emission<? extends Request>> emissions,
      @NonNull InvestigateDocument document) {
    return PipelineArchetype.super.buildEmissions(input, context, emissions, document);
  }

  private static StageOutcome outcome(PolicyDecision<?> decision) {
    return switch (decision) {
      case PolicyDecision.Next<?> ignored -> StageOutcome.NEXT;
      case PolicyDecision.Drop<?> ignored -> StageOutcome.DROP;
      case PolicyDecision.Retry<?> ignored -> StageOutcome.RETRY;
      case PolicyDecision.Error<?> ignored -> StageOutcome.ERROR;
    };
  }

  private static Classification.Collected emptyClassification() {
    return new Classification.Collected(java.util.Map.of(), 0D, java.util.Set.of());
  }
}
