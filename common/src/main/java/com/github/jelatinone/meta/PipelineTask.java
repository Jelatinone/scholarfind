package com.github.jelatinone.meta;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.meta.result.PersistResult;
import com.github.jelatinone.meta.result.PipelineResult;
import com.github.jelatinone.meta.transitory.Directive;
import com.github.jelatinone.meta.transitory.Disposition;
import com.github.jelatinone.models.audit.AttemptEvent;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.audit.StageExecution;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.StageEnvelope;
import com.github.jelatinone.policy.EmissionIntent;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyPipeline;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.RetryDirective;
import com.github.jelatinone.policy.StageOutcome;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 *
 * <h1>PipelineTask</h1>
 *
 * Describes a {@link QueueTask QueueTask} that executes a stage-specific policy
 * pipeline over a queued {@link StageEnvelope stage envelope}. This class owns
 * the stable stage algorithm: build context, build state, execute policies,
 * materialize the resulting stage document, persist control and audit records,
 * and publish downstream emissions through injected infrastructure bindings.
 *
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class PipelineTask<In extends Request, Out extends Request, Context, State, Persist extends StageDocument<Persist>, Infra extends PipelineTask.Infrastructure<In, Out>>
    extends QueueTask<StageEnvelope<In>, PipelineResult<Persist, In, Out>> {

  PipelineTask.Configuration<In, Out, Context, State, Persist, Infra> _pipelineConfig;
  Infra _infrastructure;

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration<In extends Request, Out extends Request, Context, State, Persist extends StageDocument<Persist>, Infra extends Infrastructure<In, Out>> {
    @NonNull
    PolicyPipeline<Context, State> policyPipeline;

    @NonNull
    Infra infrastructure;

    @Default
    Duration retryDuration = Duration.ofSeconds(30);

    @Default
    int transitionHistory = 25;

    @NonNull
    ProcessingStage processingStage;
  }

  public static interface Infrastructure<In extends Request, Out extends Request> extends AutoCloseable {

    RetryableQueue<StageEnvelope<In>> inQueue();

    Queue<StageEnvelope<Out>> outQueue();

    Store<AttemptEvent, String> eventStore();

    Store<StageExecution, String> executionStore();
  }

  protected PipelineTask(
      @NonNull ExecutorService executor,
      @NonNull Task.Configuration taskConfig,
      @NonNull PipelineTask.Configuration<In, Out, Context, State, Persist, Infra> pipelineConfig) {
    super(pipelineConfig.infrastructure.inQueue(), executor, taskConfig);
    this._pipelineConfig = pipelineConfig;
    this._infrastructure = pipelineConfig.infrastructure;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected final PipelineResult<Persist, In, Out> elementProcess(@NonNull StageEnvelope<In> input) {
    Instant startedAt = Instant.now();

    Context context = buildContext(input, startedAt);
    State state = buildState(context);
    PolicyDecision<State> decision = _pipelineConfig.policyPipeline.process(context, state);

    Instant occurredAt = Instant.now();
    Persist document = buildDocument(input, context, decision, occurredAt);
    PersistResult<State, Persist> persisted = persistDocument(input, context, decision, document);

    Persist persistDocument = persisted.document();
    PolicyDecision<State> persistDecision = persisted.decision();

    persistExecution(input, persistDecision, occurredAt);
    persistAttempt(input, persistDecision, startedAt, occurredAt, null);

    List<StageEnvelope<Out>> emissions = persistDecision.emissionIntents().stream()
        .map((emission) -> buildEnvelope((EmissionIntent<? extends Out>) emission, input, context, persistDecision,
            persistDocument))
        .toList();

    return new PipelineResult<>(input, persistDocument, persistDecision, emissions);
  }

  @Override
  protected final PipelineResult<Persist, In, Out> elementFailure(@NonNull StageEnvelope<In> input,
      @NonNull Throwable throwable) {
    Instant startedAt = Instant.now();

    PolicyDecision<State> decision = PolicyDecision.<State>retry(
        null,
        PolicyReason.OPERATION_EXCEPTION,
        throwable.getMessage(),
        RetryDirective.delay(_pipelineConfig.retryDuration));

    Instant occurredAt = Instant.now();

    persistExecution(input, decision, occurredAt);
    persistAttempt(input, decision, startedAt, occurredAt, throwable);
    return new PipelineResult<>(input, null, decision, List.of());
  }

  @Override
  protected final Directive elementDirective(@NonNull PipelineResult<Persist, In, Out> output) {
    return switch (output.decision().outcome()) {
      case NEXT, DROP -> Directive.COMPLETE;
      case RETRY -> Directive.RETRY;
      case ERROR -> Directive.ERROR;
    };
  }

  @Override
  protected final void onComplete(@NonNull PipelineResult<Persist, In, Out> output) throws Exception {
    if (output.emissions().isEmpty()) {
      return;
    }
    if (_infrastructure.outQueue() == null) {
      throw new IllegalStateException(
          String.format("No emission queue configured for stage %s", _pipelineConfig.processingStage));
    }
    output.emissions().forEach(_infrastructure.outQueue()::send);
  }

  @Override
  protected final void onRetry(@NonNull PipelineResult<Persist, In, Out> output) throws Exception {
    RetryDirective retryDirective = output.decision().retryDirective();
    StageEnvelope<In> retryEnvelope = retryEnvelope(output, retryDirective);
    _inQueue.sendRetry(retryEnvelope);
  }

  @Override
  protected final void onError(@NonNull PipelineResult<Persist, In, Out> output) throws Exception {
    StageEnvelope<In> errorEnvelope = errorEnvelope(output);
    _inQueue.sendError(errorEnvelope);
  }

  protected StageEnvelope<In> retryEnvelope(PipelineResult<Persist, In, Out> output, RetryDirective retryDirective) {
    In payload = output.input().payload();
    RequestHeader header = RequestHeader.retry(payload.requestHeader(), Instant.now());
    In request = buildRequest(payload, header);
    return new StageEnvelope<>(
        output.input().schemaVersion(),
        output.input().stage(),
        output.input().executionRef(),
        request.requestHeader().requestId(),
        request.target().targetId(),
        request);
  }

  protected StageEnvelope<In> errorEnvelope(PipelineResult<Persist, In, Out> output) {
    return output.input();
  }

  protected abstract Context buildContext(@NonNull StageEnvelope<In> input, @NonNull Instant startedAt);

  protected abstract State buildState(@NonNull Context context);

  protected abstract In buildRequest(In request, RequestHeader header);

  protected abstract Persist buildDocument(
      @NonNull StageEnvelope<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Instant occurredAt);

  protected abstract StageEnvelope<Out> buildEnvelope(
      @NonNull EmissionIntent<? extends Out> emission,
      @NonNull StageEnvelope<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Persist document);

  protected PersistResult<State, Persist> persistDocument(
      @NonNull StageEnvelope<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Persist document) {
    persistStageDocument(document);
    return new PersistResult<>(document, decision);
  }

  protected abstract void persistStageDocument(@NonNull Persist document);

  protected void persistExecution(
      StageEnvelope<In> input,
      PolicyDecision<?> decision,
      Instant occurredAt) {
    StageExecution current = _infrastructure.executionStore()
        .get(StageExecution.key(input.targetId(), _pipelineConfig.processingStage));
    if (current == null) {
      current = StageExecution.initial(input.targetId(), _pipelineConfig.processingStage, occurredAt);
    }

    Instant nextAttemptAt = decision.retryDirective() == null ? null : decision.retryDirective().resolve(occurredAt);
    StageExecution updated = current.transition(
        decision,
        occurredAt,
        nextAttemptAt,
        _pipelineConfig.transitionHistory,
        decision.outcome() == StageOutcome.RETRY);
    _infrastructure.executionStore().put(updated);
  }

  protected void persistAttempt(
      StageEnvelope<In> input,
      PolicyDecision<?> decision,
      Instant startedAt,
      Instant occurredAt,
      Throwable throwable) {

    Disposition disposition = switch (decision.outcome()) {
      case NEXT, DROP -> Disposition.COMPLETE;
      case RETRY -> Disposition.RETRY;
      case ERROR -> Disposition.FAIL_PERMANENT;
    };

    String fingerprint;
    if (throwable == null) {
      fingerprint = null;
    } else {
      String type = throwable.getClass().getName();
      String message = throwable.getMessage() == null ? "" : throwable.getMessage();
      fingerprint = String.format("%s:%s", type, message);
    }

    In payload = input.payload();
    RequestHeader header = payload == null
        ? null
        : payload.requestHeader();
    var target = payload == null
        ? null
        : payload.target();
    String normalizedUrl = target == null || target.normalizedUrl() == null
        ? null
        : target.normalizedUrl().toExternalForm();
    int depth = target == null
        ? -1
        : target.depth();

    AttemptEvent event = new AttemptEvent(
        input.requestId(),
        _pipelineConfig.processingStage,
        input.targetId(),
        header == null
            ? -1
            : header.attempt(),
        disposition,
        decision.reasonCodes(),
        fingerprint,
        Duration.between(startedAt, occurredAt),
        occurredAt,
        normalizedUrl,
        depth,
        header == null
            ? null
            : header.idempotencyKey());
    _infrastructure.eventStore().put(event);
  }

  @Override
  public void close() throws IOException {
    try {
      _infrastructure.close();
    } catch (Exception exception) {
      throw new IOException("Failed to close pipeline infrastructure", exception);
    }
  }
}
