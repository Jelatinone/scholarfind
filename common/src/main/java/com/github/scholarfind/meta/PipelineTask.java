package com.github.scholarfind.meta;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.github.scholarfind.api.queue.Queue;
import com.github.scholarfind.api.queue.RetryableQueue;
import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.meta.result.PipelineResult;
import com.github.scholarfind.meta.transitory.Disposition;
import com.github.scholarfind.meta.transitory.Directive;
import com.github.scholarfind.models.audit.AttemptEvent;
import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.audit.StageExecution;
import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.policy.EmissionIntent;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyPipeline;
import com.github.scholarfind.policy.PolicyReason;
import com.github.scholarfind.policy.RetryDirective;
import com.github.scholarfind.policy.StageOutcome;

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
    PolicyPipeline<Context, State> policyPipeline;
    Infra infrastructure;

    @Default
    Duration retryDuration = Duration.ofSeconds(30);

    @Default
    int transitionHistory = 25;

    ProcessingStage processingStage;
  }

  public static interface Infrastructure<In extends Request, Out extends Request> extends AutoCloseable {

    RetryableQueue<StageEnvelope<In>> inQueue();

    Queue<StageEnvelope<Out>> outQueue();

    Store<AttemptEvent, String> eventStore();

    Store<StageExecution, String> executionStore();
  }

  /**
   * Creates a new pipeline task.
   *
   * @param executor       Service to execute parallel jobs with
   * @param taskConfig     Base task configuration
   * @param pipelineConfig Stage-specific pipeline configuration including the
   *                       injected infrastructure, policy pipeline, and stage
   *                       control options
   */
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

    persistDocument(document);
    persistExecution(input, decision, occurredAt);
    persistAttempt(input, decision, startedAt, occurredAt, null);

    List<StageEnvelope<Out>> emissions = decision.emissionIntents().stream()
        .map((emission) -> buildEnvelope((EmissionIntent<? extends Out>) emission, input, context, decision, document))
        .toList();

    return new PipelineResult<>(input, document, decision, emissions);
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

  /**
   * Build a retry envelope for a retryable pipeline result.
   * 
   * @param output         Pipeline dispatch result
   * @param retryDirective Retry directive returned by the policy decision
   * @return Retryable envelope
   */
  protected StageEnvelope<In> retryEnvelope(PipelineResult<Persist, In, Out> output, RetryDirective retryDirective) {
    In payload = output.input().payload();
    RequestHeader header = new RequestHeader(
        payload.requestHeader().schemaVersion(),
        payload.requestHeader().requestId(),
        payload.requestHeader().attempt() + 1,
        payload.requestHeader().idempotencyKey(),
        Instant.now());
    In request = buildRequest(payload, header);
    return new StageEnvelope<>(
        output.input().schemaVersion(),
        output.input().stage(),
        output.input().executionRef(),
        request.requestHeader().requestId(),
        request.target().targetId(),
        request);
  }

  /**
   * Build an error envelope for a terminal failure result.
   * 
   * @param output Pipeline dispatch result
   * @return Failed envelope
   */
  protected StageEnvelope<In> errorEnvelope(PipelineResult<Persist, In, Out> output) {
    return output.input();
  }

  /**
   * Build the initial execution context of an input envelope.
   * 
   * @param input     Consumable element from the ordered source queue
   * @param startedAt Pipeline initial runtime
   * @return Initial context
   */
  protected abstract Context buildContext(@NonNull StageEnvelope<In> input, @NonNull Instant startedAt);

  /**
   * Build the initial state of a consumable element.
   * 
   * @param context Initial context of a consumable element
   * @return Initial state
   */
  protected abstract State buildState(@NonNull Context context);

  /**
   * Build the next retry request payload for a retryable consumed element.
   * 
   * @param request Initial payload
   * @param header  Next attempt request header
   * @return Retry request
   */
  protected abstract In buildRequest(In request, RequestHeader header);

  /**
   * Build the finalized, persistable document for a processed element.
   * 
   * @param input      Consumable element from the ordered source queue
   * @param context    Pipeline finalized context
   * @param decision   Pipeline finalized policy decision
   * @param occurredAt Pipeline finalized runtime
   * @return Persistable resulting document
   */
  protected abstract Persist buildDocument(
      @NonNull StageEnvelope<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Instant occurredAt);

  /**
   * Build a downstream stage envelope from a finalized emission intent.
   * 
   * @param emission Pipeline finalized emission intent
   * @param input    Consumable element from the ordered source queue
   * @param context  Pipeline finalized context
   * @param decision Pipeline finalized policy decision
   * @param document Pipeline finalized document
   * @return Resulting envelope
   */
  protected abstract StageEnvelope<Out> buildEnvelope(
      @NonNull EmissionIntent<? extends Out> emission,
      @NonNull StageEnvelope<In> input,
      @NonNull Context context,
      @NonNull PolicyDecision<State> decision,
      @NonNull Persist document);

  /**
   * Persist a successfully created document to the stage document store.
   * 
   * @param document Pipeline finalized document
   */
  protected abstract void persistDocument(@NonNull Persist document);

  /**
   * Persist the latest stage execution record for this target and stage.
   * 
   * @param input      Pipeline input envelope
   * @param decision   Pipeline finalized policy decision
   * @param occurredAt Pipeline finalized runtime
   */
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

  /**
   * Persist the per-attempt audit event for the processed envelope.
   * 
   * @param input      Pipeline input envelope
   * @param decision   Pipeline finalized policy decision
   * @param startedAt  Pipeline initial runtime
   * @param occurredAt Pipeline finalized runtime
   * @param throwable  Throwable cause for failure or retry of this attempt
   */
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

    AttemptEvent event = new AttemptEvent(
        input.requestId(),
        _pipelineConfig.processingStage,
        input.targetId(),
        input.payload().requestHeader().attempt(),
        disposition,
        decision.reasonCodes(),
        fingerprint,
        Duration.between(startedAt, occurredAt),
        occurredAt,
        String.valueOf(input.payload().target().normalizedUrl()),
        input.payload().target().depth(),
        input.payload().requestHeader().idempotencyKey());
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
