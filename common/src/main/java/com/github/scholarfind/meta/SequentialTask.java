package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.State.*;
import static org.slf4j.event.Level.*;
import static java.util.concurrent.TimeUnit.*;

import java.util.List;

import com.github.scholarfind.utility.DelayedValue;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/**
 * 
 * <h1>SequentialTask</h1>
 * 
 * Describes a {@link Task task} that can be {@link #run() operated} in
 * sequential units of execution.
 * 
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public non-sealed abstract class SequentialTask<Consumes, Produces> extends Task<Consumes, Produces> {

  @NonFinal
  Consumes operand = null;
  @NonFinal
  Produces result = null;

  /**
   * Creates a new sequential Task
   * 
   * @param config Config to associate with this task
   */
  protected SequentialTask(final @NonNull Configuration config) {
    super(config);
  }

  @Override
  public void run() {
    useMessage(String.format("Operation started : %s", _taskConfig.name), DEBUG);
    while (!_completable.isDone()) {
      try {
        State state = _state.get();
        useMessage(String.format("Operation %s : %s", state, _taskConfig.name), INFO);
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
            DelayedValue<Consumes> failed;
            int failedCount = 0;
            while ((failed = _failed.poll()) != null) {
              _collected.offer(failed.value);
              failedCount++;
            }
            useMessage(String.format("Adding failed work : %d", failedCount), DEBUG);

            Collect<Consumes> result = collect();
            switch (result) {
              case Collect.Alive(List<Consumes> collection) -> {
                useMessage(String.format("Collection shape : Alive"), INFO);

                _collected.addAll(collection);
                _collectScheduler.reset();

                useMessage(String.format("Added collected work : %d", collection.size()), DEBUG);

                useState(OPERATING);
              }

              case Collect.Idle() -> {
                useMessage(String.format("Collection shape : Idle"), INFO);
                useState(_collected.isEmpty() ? AWAITING : OPERATING);
              }

              case Collect.Empty() -> {
                useMessage(String.format("Collection shape : Empty"), INFO);
                useState(_collected.isEmpty() ? COMPLETED : OPERATING);
              }
            }
            if (_collected.size() > 0) {
              setup();
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
            Post currentStatus = post(result);

            useMessage(String.format("Posted work : %s", currentStatus), ERROR);

            switch (currentStatus) {
              case FAILURE_FATAL, SUCCESS -> {
                _attempts.remove(operand);
                useMessage(String.format("Completed work : %s", operand), DEBUG);
              }

              case FAILURE_RETRY -> {
                int attempt = _attempts.getOrDefault(operand, 0) + 1;

                if (attempt < _taskConfig.logicalRetries) {
                  long delay = _retryScheduler.compute(attempt);

                  _attempts.put(operand, attempt);
                  _failed.add(new DelayedValue<Consumes>(operand, delay, NANOSECONDS));

                  useMessage(String.format("Queued work : %s", operand), DEBUG);
                } else {
                  _attempts.remove(operand);
                }
              }
            }
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
    useMessage(String.format("Operation ended : %s", _taskConfig.name), DEBUG);
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
