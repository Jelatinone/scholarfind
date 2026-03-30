package com.github.scholarfind.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.github.scholarfind.meta.PipelineTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.annotate.AnnotateRequest;
import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.ContextDocument;
import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.models.shared.TraceReference;
import com.github.scholarfind.policy.EmissionIntent;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyPipeline;
import com.github.scholarfind.task.policy.ClassificationConfiguration;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class InvestigateTask extends
    PipelineTask<InvestigateRequest, AnnotateRequest, InvestigateContext, InvestigateState, InvestigateDocument, InvestigateInfrastructure> {

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @NonNull
    PolicyPipeline<InvestigateContext, InvestigateState> policyPipeline;

    @Builder.Default
    Duration retryTimeout = Duration.ofMinutes(5);

    @Builder.Default
    int expirationDays = 30;

    @Builder.Default
    int transitionHistory = 25;

    @Builder.Default
    int maxAttempts = 5;

    @NonNull
    ClassificationConfiguration classificationConfiguration;
  }

  Configuration _investigateConfig;

  public InvestigateTask(
      final @NonNull Task.Configuration taskConfig,
      final @NonNull Configuration investigateConfig,
      final @NonNull InvestigateInfrastructure infrastructure,
      final @NonNull ExecutorService executor) {
    super(
        executor,
        taskConfig,
        PipelineTask.Configuration
            .<InvestigateRequest, AnnotateRequest, InvestigateContext, InvestigateState, InvestigateDocument, InvestigateInfrastructure>builder()
            .policyPipeline(investigateConfig.policyPipeline)
            .infrastructure(infrastructure)
            .retryDuration(investigateConfig.retryTimeout)
            .transitionHistory(investigateConfig.transitionHistory)
            .processingStage(ProcessingStage.INVESTIGATE)
            .build());
    _investigateConfig = investigateConfig;
  }

  @Override
  protected InvestigateContext buildContext(
      @NonNull StageEnvelope<InvestigateRequest> input,
      @NonNull Instant startedAt) {
    InvestigateRequest request = input.payload();
    InvestigateDocument currentDocument = InvestigateDocument.builder()
        .documentHeader(new DocumentHeader(
            InvestigateDocument.SCHEMA_VERSION,
            UUID.randomUUID(),
            request.requestHeader().requestId(),
            request.target().targetId(),
            startedAt))
        .requestHeader(request.requestHeader())
        .target(request.target())
        .trace(new TraceReference(
            request.target().normalizedUrl(),
            null,
            InvestigateTask.class.getSimpleName(),
            request.target().depth()))
        .reviewedAt(startedAt)
        .classification(new Classification(Map.of()))
        .confidence(0D)
        .discoveredTargetCount(0)
        .build();

    InvestigateDocument retrievedInvestigate = _infrastructure.investigateStore().get(request.target().targetId());
    ContextDocument retrievedContext = _infrastructure.contextStore().get(request.target().targetId());

    return new InvestigateContext(
        currentDocument,
        _investigateConfig.classificationConfiguration,
        startedAt,
        retrievedInvestigate,
        retrievedContext);
  }

  @Override
  protected InvestigateState buildState(@NonNull InvestigateContext context) {
    return InvestigateState.initial(context.reviewedAt());
  }

  @Override
  protected InvestigateDocument buildDocument(
      @NonNull StageEnvelope<InvestigateRequest> input,
      @NonNull InvestigateContext context,
      @NonNull PolicyDecision<InvestigateState> decision,
      @NonNull Instant occurredAt) {
    return new InvestigateDocument(
        context.document().documentHeader(),
        context.document().requestHeader(),
        context.document().target(),
        context.document().trace(),
        occurredAt,
        decision.state() == null
            ? context.document().classification()
            : decision.state().classification(),
        decision.state() == null
            ? context.document().confidence()
            : decision.state().confidence(),
        decision.state() == null
            ? context.document().discoveredTargetCount()
            : decision.state().discoveredTargetCount());
  }

  @Override
  protected void persistDocument(@NonNull InvestigateDocument document) {
    _infrastructure.investigateStore().put(document);
  }

  @Override
  protected StageEnvelope<AnnotateRequest> buildEnvelope(
      @NonNull EmissionIntent<? extends AnnotateRequest> emission,
      @NonNull StageEnvelope<InvestigateRequest> input,
      @NonNull InvestigateContext context,
      @NonNull PolicyDecision<InvestigateState> decision,
      @NonNull InvestigateDocument document) {
    return StageEnvelope.of(
        ProcessingStage.ANNOTATE,
        document.documentHeader().documentId().toString(),
        emission.request());
  }

  @Override
  protected InvestigateRequest buildRequest(InvestigateRequest request, RequestHeader nextHeader) {
    return new InvestigateRequest(nextHeader, request.target());
  }
}
