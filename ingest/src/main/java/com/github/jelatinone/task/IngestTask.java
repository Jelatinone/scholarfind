package com.github.jelatinone.task;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.meta.ParallelTask;
import com.github.jelatinone.meta.PipelineTask;
import com.github.jelatinone.meta.Task;
import com.github.jelatinone.meta.result.PersistResult;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.ingest.IngestDecision;
import com.github.jelatinone.models.ingest.IngestDocument;
import com.github.jelatinone.models.ingest.IngestProvenance;
import com.github.jelatinone.models.ingest.IngestRequest;
import com.github.jelatinone.models.ingest.TargetRecord;
import com.github.jelatinone.models.investigate.InvestigateRequest;
import com.github.jelatinone.models.shared.DocumentHeader;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.StageEnvelope;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyPipeline;
import com.github.jelatinone.task.policy.IngestPolicyReason;

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

    IngestInfrastructure infrastructure;

    @Builder.Default
    Duration retryTimeout = Duration.ofMinutes(5);

    @Builder.Default
    Duration expirationTimeout = Duration.ofDays(30);

    @Builder.Default
    Duration rescheduleTimeout = Duration.ofHours(24);

    @Builder.Default
    int transitionHistory = 25;

    @Builder.Default
    int maxDepth = 5;

    @Builder.Default
    int maxAttempts = 5;
  }

  Configuration _ingestConfig;

  private IngestTask(
      final IngestTask.Configuration ingestConfig,
      final ParallelTask.Configuration parallelConfig,
      final Task.Configuration taskConfig) {
    super(
        PipelineTask.Configuration
            .<IngestRequest, InvestigateRequest, IngestContext, IngestState, IngestDocument, IngestInfrastructure>builder()
            .policyPipeline(ingestConfig.policyPipeline)
            .infrastructure(ingestConfig.infrastructure)
            .retryDuration(ingestConfig.retryTimeout)
            .transitionHistory(ingestConfig.transitionHistory)
            .processingStage(ProcessingStage.INGEST)
            .build(),
        parallelConfig,
        taskConfig);
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
        _ingestConfig.rescheduleTimeout,
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
  protected PersistResult<IngestState, IngestDocument> persistDocument(
      @NonNull StageEnvelope<IngestRequest> input,
      @NonNull IngestContext context,
      @NonNull PolicyDecision<IngestState> decision,
      @NonNull IngestDocument document) {
    TargetRecord nextTarget = buildTarget(context.targetRecord(), document);
    IngestPersistResult result = _infrastructure.persist(document, context.targetRecord(), nextTarget);
    return switch (result) {
      case APPLIED -> new PersistResult<>(document, decision);
      case ADMISSION_CONFLICT -> suppressConflict(context, decision, document);
    };
  }

  @Override
  protected void persistStageDocument(@NonNull IngestDocument document) {
    throw new UnsupportedOperationException("Ingest persistence requires contextual target-record coordination");
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <Emit extends Request> StageEnvelope<Emit> buildEnvelope(
      @NonNull Emission<? extends Request> emission,
      @NonNull StageEnvelope<IngestRequest> input,
      @NonNull IngestContext context,
      @NonNull PolicyDecision<IngestState> decision,
      @NonNull IngestDocument document) {
    Instant emittedAt = Instant.now();
    return switch (emission.forwardRef()) {
      case INVESTIGATE -> {
        if (!(emission.request() instanceof InvestigateRequest request)) {
          throw unsupportedEmission(emission);
        }
        RequestHeader nextHeader = RequestHeader.next(
            document.requestHeader(),
            InvestigateRequest.SCHEMA_VERSION,
            emittedAt);
        InvestigateRequest routed = new InvestigateRequest(nextHeader, request.target());

        yield StageEnvelope.of(
            ProcessingStage.INVESTIGATE,
            document.documentHeader().documentId().toString(),
            (Emit) routed);
      }
      default -> throw unsupportedEmission(emission);
    };
  }

  @Override
  protected IngestRequest buildRequest(IngestRequest request, RequestHeader nextHeader) {
    return new IngestRequest(nextHeader, request.target(), request.provenance(), request.priority());
  }

  private PersistResult<IngestState, IngestDocument> suppressConflict(
      IngestContext context,
      PolicyDecision<IngestState> decision,
      IngestDocument document) {
    TargetRecord refreshedTarget = retrieveTarget(document.target());
    IngestState suppressedState = decision.state() == null
        ? IngestState.initial(context.reviewedAt(), document.depthBudget())
        : decision.state();
    suppressedState = suppressedState.withDecision(IngestDecision.DUPLICATE_SUPPRESSED);

    IngestDocument suppressedDocument = new IngestDocument(
        document.documentHeader(),
        document.requestHeader(),
        document.target(),
        document.provenance(),
        document.priority(),
        IngestDecision.DUPLICATE_SUPPRESSED,
        document.depthBudget());

    TargetRecord suppressedTarget = buildTarget(refreshedTarget, suppressedDocument);
    IngestPersistResult persistResult = _infrastructure.persist(suppressedDocument, refreshedTarget,
        suppressedTarget);
    if (persistResult != IngestPersistResult.APPLIED) {
      throw new IllegalStateException(
          "Failed to persist duplicate-suppressed ingest document after admission conflict");
    }

    PolicyDecision<IngestState> suppressedDecision = PolicyDecision.drop(
        suppressedState,
        IngestPolicyReason.TARGET_DUPLICATE_SUPPRESSED,
        "Concurrent ingest admission conflict suppressed a duplicate target");
    return new PersistResult<>(suppressedDocument, suppressedDecision);
  }

  private TargetRecord retrieveTarget(TargetReference target) {
    if (target == null || target.normalizedUrl() == null) {
      return null;
    }
    return _infrastructure.targetStore().get(TargetRecord.key(target));
  }

  private TargetRecord buildTarget(TargetRecord current, IngestDocument document) {
    if (document.decision() == IngestDecision.INVALID_TARGET
        || document.target() == null
        || document.target().normalizedUrl() == null) {
      return null;
    }
    return TargetRecord.upsert(current, document);
  }
}
