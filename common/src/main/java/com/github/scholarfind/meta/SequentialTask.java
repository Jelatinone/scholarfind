package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.State.*;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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
  static Logger _logger = Logger.getLogger(SequentialTask.class.getName());

  AtomicBoolean _lastOk;
  AtomicInteger _attempt;

  @NonFinal
  ListIterator<Consumes> iterator;
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
    _lastOk = new AtomicBoolean();
    _attempt = new AtomicInteger();
  }

  @Override
  public synchronized void run() {
    iterator = null;
    _lastOk.set(true);
    while (!_completable.isDone()) {
      try {
        State state = _state.get();
        switch (state) {

          case CREATED -> {
            useState(COLLECTING);
          }

          case AWAITING -> {
            await();
          }

          case COLLECTING -> {
            CollectionResult<Consumes> data = collect();
            switch (data) {
              case CollectionResult.Alive(List<Consumes> collection) -> {
                iterator = collection.listIterator();
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
            if (!iterator.hasNext()) {
              useState(COLLECTING);
              break;
            }
            result = operate(operand = iterator.next());
            useState(POSTING);
          }

          case POSTING -> {
            boolean currentStatus = post(result);
            boolean previousStatus = _lastOk.get();
            if (!previousStatus && currentStatus) {
              _attempt.set(0);
            }
            if (!currentStatus) {
              useState(RETRYING);
            } else {
              useState(OPERATING);
            }
            _lastOk.set(currentStatus);
          }

          case RESTARTING -> {
            restart();
            useState(CREATED);
            _attempt.set(0);
          }

          case RETRYING -> {
            final int currentAttempt = _attempt.getAndIncrement();
            if (currentAttempt >= _options.operandRetires) {
              _attempt.set(0);
              if (iterator.hasNext()) {
                useState(OPERATING);
              } else {
                useState(COLLECTING);
              }
            } else {
              iterator.previous();
              useState(OPERATING);
            }
          }

          case COMPLETED, FAILED -> {
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
        _completable.completeExceptionally(throwable);
        throwable.printStackTrace();
      }
    }
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
