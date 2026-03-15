package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.result.PostResult.FAILURE_FATAL;
import static com.github.scholarfind.meta.result.PostResult.FAILURE_RETRY;
import static com.github.scholarfind.meta.result.PostResult.SUCCESS;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.github.scholarfind.api.queue.QueueResult;
import com.github.scholarfind.api.queue.RetryableQueue;
import com.github.scholarfind.meta.result.CollectionResult;
import com.github.scholarfind.meta.result.OperationResult;
import com.github.scholarfind.meta.result.PostResult;
import com.github.scholarfind.utility.Envelope;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 *
 * <h1>QueueTask</h1>
 *
 * <p>
 * Describes a {@link ParallelTask ParallelTask} whose consumed elements are
 * sourced from a {@link RetryableQueue retryable queue}.
 *
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class QueueTask<Consumes, Produces> extends ParallelTask<Envelope<Consumes>, Envelope<Produces>> {

  RetryableQueue<Consumes> _queue;

  /**
   * Creates a new queue Task
   * 
   * @param executor   Service to execute parallel jobs with
   * @param taskConfig Config to associate with this task
   * @param queue      Queue to pull elements from
   */
  protected QueueTask(@NonNull ExecutorService executor, @NonNull RetryableQueue<Consumes> queue,
      @NonNull Configuration taskConfig) {
    super(executor, taskConfig);
    _queue = queue;
  }

  @Override
  protected final @NonNull CollectionResult<Envelope<Consumes>> collect() {
    QueueResult<Consumes> receivedMessages = _queue.poll(_taskConfig.collectionSize);
    List<Envelope<Consumes>> envelopes = receivedMessages.messages().stream()
        .map((message) -> new Envelope<>(message.message(), message.acknowledgement()))
        .toList();
    if (!envelopes.isEmpty()) {
      return new CollectionResult.Alive<>(envelopes);
    }
    return switch (receivedMessages.state()) {
      case ACTIVE, IDLE -> new CollectionResult.Idle<>();
      case EMPTY -> new CollectionResult.Empty<>();
    };
  }

  @Override
  protected final OperationResult<Envelope<Produces>> operate(@NonNull Envelope<Consumes> operand) {
    try {
      Produces output = elementProcess(operand.document());
      return new OperationResult<>(new Envelope<>(output, operand.acknowledgement()));
    } catch (Throwable throwable) {
      useMessage(String.format("Queue operation failed : %s", throwable.getMessage()), ERROR, throwable);
      Produces output = elementFailure(operand.document(), throwable);
      return new OperationResult<>(new Envelope<>(output, operand.acknowledgement()));
    }
  }

  @Override
  protected final @NonNull PostResult post(OperationResult<Envelope<Produces>> operand) {
    if (operand == null || operand.value() == null || operand.value().document() == null) {
      useMessage(String.format("Queue operation result was null : %s", operand), DEBUG);
      return FAILURE_FATAL;
    }

    Envelope<Produces> envelope = operand.value();
    Produces output = envelope.document();
    try {
      switch (elementDirective(output)) {
        case COMPLETE -> {
          onComplete(output);
          envelope.acknowledgement().success();
        }
        case RETRY -> {
          onRetry(output);
          envelope.acknowledgement().success();
        }
        case ERROR -> {
          onError(output);
          envelope.acknowledgement().success();
        }
      }
      return SUCCESS;
    } catch (Exception exception) {
      useMessage(String.format("Queue post failed : %s", exception.getMessage()), ERROR, exception);
      return FAILURE_RETRY;
    }
  }

  /**
   * Process a consumable queue element into a produced element.
   * 
   * @param input Consumable element from the ordered source queue
   * @return Produced element
   */
  protected abstract Produces elementProcess(@NonNull Consumes input);

  /**
   * Handle an exceptional {@link #elementProcess(Object)} invocation and map it
   * to a valid produced element that can continue through the post phase.
   * 
   * @param input     Consumable element from the ordered source queue
   * @param throwable Throwable raised during {@link #elementProcess(Object)}
   *                  processing
   * @return Produced element
   */
  protected abstract Produces elementFailure(@NonNull Consumes input, @NonNull Throwable throwable);

  /**
   * Resolve the directive that controls which completion hook runs and how the
   * original queue message is acknowledged.
   * 
   * @param output Output of the {@link #operate(Envelope) operation} stage
   * @return Decision on action to take for this element within the queue
   */
  protected abstract QueueDirective elementDirective(@NonNull Produces output);

  /**
   * Handle the completion path for an element whose directive resolved to
   * {@link QueueDirective#COMPLETE}.
   * 
   * @param output Output of the {@link #operate(Envelope) operation} stage
   * @throws Exception When an exception has occurred during completion processing
   */
  protected void onComplete(@NonNull Produces output) throws Exception {
  }

  /**
   * Handle the retry path for an element whose directive resolved to
   * {@link QueueDirective#RETRY}. Implementations should explicitly publish the
   * next retry message here when needed.
   * 
   * @param output Output of the {@link #operate(Envelope) operation} stage
   * @throws Exception When an exception has occurred during retry processing
   */
  protected void onRetry(@NonNull Produces output) throws Exception {
  }

  /**
   * Handle the terminal error path for an element whose directive resolved to
   * {@link QueueDirective#ERROR}. Implementations should explicitly publish any
   * dead-letter or quarantine message here when needed.
   * 
   * @param output Output of the {@link #operate(Envelope) operation} stage
   * @throws Exception When an exception has occurred during failure processing
   */
  protected void onError(@NonNull Produces output) throws Exception {
  }
}
