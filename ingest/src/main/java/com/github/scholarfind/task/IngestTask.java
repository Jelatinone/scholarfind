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
import com.github.scholarfind.models.ingest.IngestProvenance;
import com.github.scholarfind.models.ingest.IngestRequest;
import com.github.scholarfind.models.ingest.TargetRecord;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.models.shared.TargetReference;
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
    RequestHeader requestHeader = request == null
        ? null
        : request.requestHeader();
    TargetReference target = request == null
        ? null
        : request.target();
    IngestProvenance provenance = request == null
        ? null
        : request.provenance();
    Integer priority = request == null
        ? null
        : request.priority();
    UUID requestId = requestHeader != null
        ? requestHeader.requestId()
        : input.requestId();
    UUID targetId = target != null
        ? target.targetId()
        : input.targetId();
    Integer depthBudget = target == null
        ? _ingestConfig.maxDepth
        : Math.max(0, _ingestConfig.maxDepth - target.depth());

    IngestDocument currentDocument = new IngestDocument(
        new DocumentHeader(
            IngestDocument.SCHEMA_VERSION,
            UUID.randomUUID(),
            requestId,
            targetId,
            startedAt),
        requestHeader,
        target,
        provenance,
        priority,
        IngestDecision.PENDING,
        depthBudget);

    TargetRecord targetRecord = retrieveTarget(target);
    return new IngestContext(
        currentDocument,
        startedAt,
        targetRecord,
        _ingestConfig.maxDepth,
        _ingestConfig.rescheduleCooldown,
        input.schemaVersion(),
        input.stage(),
        input.requestId(),
        input.targetId());
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

    IngestState state = decision.state() == null
        ? IngestState.initial(context.reviewedAt(), context.document().depthBudget())
        : decision.state();

    DocumentHeader header = context.document().documentHeader();
    DocumentHeader nextHeader = new DocumentHeader(
        header.schemaVersion(),
        header.documentId(),
        header.requestId(),
        header.targetId(),
        occurredAt);

    return new IngestDocument(
        nextHeader,
        context.document().requestHeader(),
        context.document().target(),
        context.document().provenance(),
        context.document().priority(),
        state.decision(),
        state.depthBudget());
  }

  @Override
  protected void persistDocument(@NonNull IngestDocument document) {
    TargetRecord target = buildTarget(document);
    _infrastructure.persist(document, target);
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
    return new IngestRequest(nextHeader, request.target(), request.provenance(), request.priority());
  }

  private TargetRecord retrieveTarget(TargetReference target) {
    if (target == null || target.normalizedUrl() == null) {
      return null;
    }
    return _infrastructure.targetStore().get(TargetRecord.key(target));
  }

  private TargetRecord buildTarget(IngestDocument document) {
    if (document.decision() == IngestDecision.INVALID_TARGET
        || document.target() == null
        || document.target().normalizedUrl() == null) {
      return null;
    }

    TargetRecord current = _infrastructure.targetStore().get(TargetRecord.key(document.target()));
    return TargetRecord.upsert(current, document);
  }
}
