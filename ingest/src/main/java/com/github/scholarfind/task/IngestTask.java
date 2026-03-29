package com.github.scholarfind.task;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.github.scholarfind.meta.PipelineTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.ingest.IngestDecision;
import com.github.scholarfind.models.ingest.IngestDocument;
import com.github.scholarfind.models.ingest.IngestRequest;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.policy.EmissionIntent;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyPipeline;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class IngestTask extends
    PipelineTask<IngestRequest, InvestigateRequest, IngestContext, IngestState, IngestDocument, IngestInfrastructure> {

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    PolicyPipeline<IngestContext, IngestState> policyPipeline;

    @Builder.Default
    Duration retryTimeout = Duration.ofMinutes(5);

    @Builder.Default
    int expirationDays = 30;

    @Builder.Default
    int transitionHistory = 25;

    @Builder.Default
    int maxDepth = 5;

    @Builder.Default
    Duration rescheduleCooldown = Duration.ofHours(24);
  }

  Configuration _ingestConfig;

  public IngestTask(
      final @NonNull Task.Configuration taskConfig,
      final @NonNull Configuration ingestConfig,
      final @NonNull IngestInfrastructure infrastructure,
      final @NonNull ExecutorService executor) {
    super(
        executor,
        taskConfig,
        PipelineTask.Configuration
            .<IngestRequest, InvestigateRequest, IngestContext, IngestState, IngestDocument, IngestInfrastructure>builder()
            .policyPipeline(ingestConfig.policyPipeline)
            .infrastructure(infrastructure)
            .retryDuration(ingestConfig.retryTimeout)
            .transitionHistory(ingestConfig.transitionHistory)
            .processingStage(ProcessingStage.INGEST)
            .build());
    _ingestConfig = ingestConfig;
  }

  @Override
  protected IngestContext buildContext(
      @NonNull StageEnvelope<IngestRequest> input,
      @NonNull Instant startedAt) {
    IngestRequest request = input.payload();
    IngestDocument currentDocument = new IngestDocument(
        new DocumentHeader(
            IngestDocument.schemaVersion,
            UUID.randomUUID(),
            request.requestHeader().requestId(),
            request.target().targetId(),
            startedAt),
        request.requestHeader(),
        request.target(),
        request.origin(),
        IngestDecision.PENDING,
        Math.max(0, _ingestConfig.maxDepth - request.target().depth()));

    IngestDocument retrievedIngest = _infrastructure.ingestStore().get(request.target().targetId());
    return new IngestContext(
        currentDocument,
        startedAt,
        retrievedIngest,
        _ingestConfig.maxDepth,
        _ingestConfig.rescheduleCooldown);
  }

  @Override
  protected IngestState buildState(@NonNull IngestContext context) {
    return IngestState.initial(context.reviewedAt(), context.document().depthBudget());
  }

  @Override
  protected IngestDocument buildDocument(
      @NonNull StageEnvelope<IngestRequest> input,
      @NonNull IngestContext context,
      @NonNull PolicyDecision<IngestState> decision,
      @NonNull Instant occurredAt) {
    IngestState finalState = decision.state() == null
        ? IngestState.initial(context.reviewedAt(), context.document().depthBudget())
        : decision.state();

    return new IngestDocument(
        context.document().documentHeader(),
        context.document().requestHeader(),
        context.document().target(),
        context.document().origin(),
        finalState.decision(),
        finalState.depthBudget());
  }

  @Override
  protected void persistDocument(@NonNull IngestDocument document) {
    _infrastructure.ingestStore().put(document);
  }

  @Override
  protected StageEnvelope<InvestigateRequest> buildEnvelope(
      @NonNull EmissionIntent<? extends InvestigateRequest> emission,
      @NonNull StageEnvelope<IngestRequest> input,
      @NonNull IngestContext context,
      @NonNull PolicyDecision<IngestState> decision,
      @NonNull IngestDocument document) {
    return StageEnvelope.of(
        ProcessingStage.INVESTIGATE,
        document.documentHeader().documentId().toString(),
        emission.request());
  }

  @Override
  protected IngestRequest buildRequest(IngestRequest request, RequestHeader nextHeader) {
    return new IngestRequest(nextHeader, request.target(), request.origin(), request.priority());
  }
}
