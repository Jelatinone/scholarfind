package com.github.jelatinone.meta;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.github.jelatinone.meta.construct.Infrastructure;
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
public abstract class PipelineTask<In extends Request, Out extends Request, Context, State, Persist extends StageDocument<Persist>, Infra extends Infrastructure<In, Out>>
		extends QueueTask<StageEnvelope<In>, PipelineResult<Persist, In>> {

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

	protected PipelineTask(
			@NonNull PipelineTask.Configuration<In, Out, Context, State, Persist, Infra> pipelineConfig,
			@NonNull ParallelTask.Configuration parallelConfig,
			@NonNull Task.Configuration taskConfig) {
		super(pipelineConfig.infrastructure.input(), parallelConfig, taskConfig);
		this._pipelineConfig = pipelineConfig;
		this._infrastructure = pipelineConfig.infrastructure;
	}

	@Override
	protected final PipelineResult<Persist, In> elementProcess(@NonNull StageEnvelope<In> input) {
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

		List<StageEnvelope<Request>> emissions = persistDecision.emissionIntents().stream()
				.map((emission) -> buildEnvelope(emission, input, context, persistDecision, persistDocument))
				.toList();

		return new PipelineResult<>(input, persistDocument, persistDecision, emissions);
	}

	@Override
	protected final PipelineResult<Persist, In> elementFailure(@NonNull StageEnvelope<In> input,
			@NonNull Throwable throwable) {
		Instant startedAt = Instant.now();

		PolicyDecision<State> decision = PolicyDecision.retry(
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
	protected final Directive elementDirective(@NonNull PipelineResult<Persist, In> output) {
		return switch (output.decision().outcome()) {
			case NEXT, DROP -> Directive.COMPLETE;
			case RETRY -> Directive.RETRY;
			case ERROR -> Directive.ERROR;
		};
	}

	@Override
	protected final void onComplete(@NonNull PipelineResult<Persist, In> output) {
		if (output.emissions().isEmpty()) {
			return;
		}
		output.emissions().forEach(_infrastructure.output()::route);
	}

	@Override
	protected final void onRetry(@NonNull PipelineResult<Persist, In> output) {
		RetryDirective retryDirective = output.decision().retryDirective();
		StageEnvelope<In> retryEnvelope = retryEnvelope(output, retryDirective);
		_inQueue.sendRetry(retryEnvelope);
	}

	@Override
	protected final void onError(@NonNull PipelineResult<Persist, In> output) {
		StageEnvelope<In> errorEnvelope = errorEnvelope(output);
		_inQueue.sendError(errorEnvelope);
	}

	/**
	 * Generate an envelope corresponding to a retryable failure of an
	 * {@link #elementProcess(StageEnvelope) process result} triggered by an
	 * {@link #onRetry(PipelineResult) retry decision}
	 * 
	 * @param output         Pipeline result yielded by
	 *                       {@link #persistDocument(StageEnvelope, Object, PolicyDecision, StageDocument)
	 *                       persisting} the result of the policy pipeline on the
	 *                       {@link #collect() input}
	 * @param retryDirective Retry decision yielded by
	 *                       {@link #persistDocument(StageEnvelope, Object, PolicyDecision, StageDocument)
	 *                       persisting} the result of the policy pipeline on the
	 *                       {@link #collect() input}
	 * @return Envelope to send into the retry queue
	 */
	protected StageEnvelope<In> retryEnvelope(PipelineResult<Persist, In> output, RetryDirective retryDirective) {
		In payload = output.input().payload();

		RequestHeader nextHeader = RequestHeader.retry(payload.requestHeader(), Instant.now());
		In nextRequest = buildRequest(payload, nextHeader);
		return new StageEnvelope<>(
				output.input().schemaVersion(),
				output.input().stage(),
				output.input().executionRef(),
				nextRequest.requestHeader().requestId(),
				nextRequest.target().targetId(),
				nextRequest);
	}

	/**
	 * Generate an envelope corresponding to an operational failure of an
	 * {@link #elementProcess(StageEnvelope) process result} triggered by an
	 * {@link #onError(PipelineResult) error decision}
	 * 
	 * @param output Pipeline result yielded by
	 *               {@link #persistDocument(StageEnvelope, Object, PolicyDecision, StageDocument)
	 *               persisting} the result of the policy pipeline on the
	 *               {@link #collect() input}
	 * @return Envelope to send into the error queue
	 */
	protected StageEnvelope<In> errorEnvelope(PipelineResult<Persist, In> output) {
		return output.input();
	}

	/**
	 * Generate an initial context to seed the policy pipeline with
	 * 
	 * @param input     Pipeline input yielded by {@link #collect() collection}
	 * @param startedAt Chronological instant of context creation
	 * @return Valid initial context corresponding to the provided envelope
	 */
	protected abstract Context buildContext(@NonNull StageEnvelope<In> input, @NonNull Instant startedAt);

	/**
	 * Generate an initial, modifiable state corresponding to context
	 * 
	 * @param context Generated context
	 * @return Valid initial state corresponding to the provided envelope
	 */
	protected abstract State buildState(@NonNull Context context);

	/**
	 * Generate an input request
	 * 
	 * @param request Pipeline input request yielded by {@link #collect()
	 *                collection}
	 * @param header  Generated header yielded by upsetting current chronological
	 *                time into the original request
	 * @return Valid retry request
	 */
	protected abstract In buildRequest(In request, RequestHeader header);

	/**
	 * Generate a persist-able result
	 * 
	 * @param input      Pipeline input yielded by {@link #collect() collection}
	 * @param context    Generated context
	 * @param decision   Pipeline output decision
	 * @param occurredAt Chronological instant of output persist
	 * @return Valid persist document
	 */
	protected abstract Persist buildDocument(
			@NonNull StageEnvelope<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull Instant occurredAt);

	/**
	 * Build a generalized envelope supported by this pipeline
	 * 
	 * @param <Emit>   Any descendant of {@link Request}
	 * @param emission Pipeline output emission
	 * @param input    Pipeline input yielded by {@link #collect() collection}
	 * @param context  Generated context
	 * @param decision Pipeline output decision
	 * @param document Generated Document
	 * @return Valid general envelope descending from the
	 *         {@link EmissionIntent#forwardRef() forwarding reference}
	 * 
	 * @throws IllegalArgumentException When an invalid envelope has been requested
	 */
	protected abstract <Emit extends Request> StageEnvelope<Emit> buildEnvelope(
			@NonNull EmissionIntent<? extends Request> emission,
			@NonNull StageEnvelope<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull Persist document);

	/**
	 * Generate an unsupported emission
	 * 
	 * @apiNote Helper intended for usage within the implementation of
	 *          {@link #buildEnvelope(EmissionIntent, StageEnvelope, Object, PolicyDecision, StageDocument)}
	 * 
	 * @param emission Pipeline output emission
	 * @return Unsupported emission exception
	 */
	protected IllegalArgumentException unsupportedEmission(EmissionIntent<? extends Request> emission) {
		return new IllegalArgumentException(String.format("%s cannot route emission stage=%s payload=%s",
				_taskConfig.name,
				emission.forwardRef(),
				emission.request() == null
						? "null"
						: emission.request().getClass().getName()));
	}

	/**
	 * Persist a valid generated by policy pipeline document
	 * 
	 * @param input    Pipeline input yielded by {@link #collect() collection}
	 * @param context  Generated context
	 * @param decision Pipeline output decision
	 * @param document Generated Document
	 * @return Persist operational result
	 */
	protected PersistResult<State, Persist> persistDocument(
			@NonNull StageEnvelope<In> input,
			@NonNull Context context,
			@NonNull PolicyDecision<State> decision,
			@NonNull Persist document) {
		persistStageDocument(document);
		return new PersistResult<>(document, decision);
	}

	/**
	 * Persist a valid document for this stage using configured infrastructure
	 * 
	 * @param document Generated Document
	 */
	protected abstract void persistStageDocument(@NonNull Persist document);

	/**
	 * Persist a stage execution event result into the execution store
	 * 
	 * @param input      Pipeline input yielded by {@link #collect() collection}
	 * @param decision   Pipeline output decision
	 * @param occurredAt Chronological instant of output persist
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
	 * Persist a stage attempt event result into the attempt store
	 * 
	 * @param input      Pipeline input yielded by {@link #collect() collection}
	 * @param decision   Pipeline output decision
	 * @param startedAt  Chronological instant of
	 *                   {@link #operate(com.github.jelatinone.api.Envelope)
	 *                   operation} initialization
	 * @param occurredAt Chronological instant of output persist
	 * @param throwable  Optional primary cause of failure
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
