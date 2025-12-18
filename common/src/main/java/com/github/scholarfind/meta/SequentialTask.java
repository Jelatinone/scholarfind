package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.State.*;
import static org.slf4j.event.Level.*;
import static java.util.concurrent.TimeUnit.*;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/**
 * 
 * <h1>ParallelTask</h1>
 * 
 * Describes a {@link Task task} that can be {@link #run() operated} in
 * sequential units of execution.
 * 
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public non-sealed abstract class SequentialTask<Consumes, Produces> extends Task<Consumes, Produces> {
  static Logger _logger = LoggerFactory.getLogger(SequentialTask.class);

  @NonFinal
  Consumes operand = null;
  @NonFinal
  Produces result = null;

  /**
   * Creates a new sequential Task
   * 
   * @param name Name of the task to be created
   */
  protected SequentialTask(final @NonNull String name) {
    this(name, Options.builder().build());
  }

  /**
   * Creates a new sequential Task
   * 
   * @param name    Name of the task to be created
   * @param options Options to associate with this task
   */
  protected SequentialTask(final @NonNull String name, final @NonNull Options options) {
    super(name, options);
  }

  @Override
  public void run() {
    useMessage(String.format("Operation started : %s", _name), DEBUG);
    while (!_completable.isDone()) {
      try {
        State state = _state.get();
        useMessage(String.format("Operation %s : %s", state, _name), INFO);
        switch (state) {

          case CREATED -> {
            setup();
            useState(COLLECTING);
          }

          case AWAITING -> {
            await();
            useState(COLLECTING);
          }

          case COLLECTING -> {
            CollectionResult<Consumes> result = collect();
            switch (result) {
              case CollectionResult.Alive(Queue<Consumes> collection) -> {
                useMessage(String.format("Adding new jobs : %d", collection.size()), DEBUG);
                _collected.addAll(collection);
                _collectScheduler.reset();

                useMessage(String.format("Adding failed jobs : %d", _failed.size()), DEBUG);
                DelayedValue<Consumes> failed;
                while ((failed = _failed.poll()) != null) {
                  _collected.offer(failed.operand);
                }

                useState(OPERATING);
              }

              case CollectionResult.Idle() -> {
                useState(AWAITING);
              }

              case CollectionResult.Empty() -> {
                useState(COMPLETED);
              }
            }
          }

          case OPERATING -> {
            operand = _collected.poll();
            if (operand == null) {
              useState(COLLECTING);
              break;
            }
            result = operate(operand);
            useState(POSTING);
          }

          case POSTING -> {
            boolean currentStatus = post(result);
            if (!currentStatus) {
              int attempt = _attempts.getOrDefault(operand, 0) + 1;
              useMessage(String.format("Failed dispatched job : %s", operand.toString()), ERROR);
              if (attempt < _options.operandRetires) {
                long delay = _retryScheduler.compute(attempt);
                _attempts.put(operand, attempt + 1);
                _failed.add(new DelayedValue<Consumes>(operand, delay, NANOSECONDS));
                useMessage(String.format("Queued dispatched job : %s", operand.toString()), DEBUG);
              }
            } else {
              useMessage(String.format("Completed dispatched job : %s", operand.toString()), INFO);
              _attempts.remove(operand);
            }
            useState(OPERATING);
          }

          case RESTARTING -> {
            restart();
            _attempts.clear();
            _failed.clear();
            useState(CREATED);
          }

          case COMPLETED, FAILED -> {
            shutdown();
            _completable.complete(null);
            return;
          }

          default -> {
            _completable.complete(null);
            throw new IllegalStateException("Accessed invalid state during SequentialTask execution.");
          }
        }
      } catch (final Throwable throwable) {
        useState(FAILED);
        useMessage(String.format("Operation interrupted : %s", throwable.getCause()), ERROR, throwable);
        _completable.completeExceptionally(throwable);
      }
    }
    useMessage(String.format("Operation ended : %s", _name), DEBUG);
  }

  /**
   * Provides the most recent consumed operand
   * 
   * @return Previous consumed operand
   */
  public Consumes getConsumed() {
    return operand;
  }

  /**
   * Provides the most recent produced operand
   * 
   * @return Previous produced operand
   */
  public Produces getProduced() {
    return result;
  }
}
