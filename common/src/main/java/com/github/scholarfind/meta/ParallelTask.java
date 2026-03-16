package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.transitory.State.*;
import static java.util.concurrent.TimeUnit.*;
import static org.slf4j.event.Level.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import com.github.scholarfind.meta.result.CollectionResult;
import com.github.scholarfind.meta.result.OperationResult;
import com.github.scholarfind.meta.result.PostResult;
import com.github.scholarfind.meta.transitory.State;
import com.github.scholarfind.utility.Locked;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 * 
 * <h1>ParallelTask</h1>
 * 
 * Describes a {@link Task task} that can be {@link #run() operated} in parallel
 * units of execution.
 * 
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public non-sealed abstract class ParallelTask<Consumes, Produces> extends Task<Consumes, Produces> {

  ExecutorService _executor;
  Semaphore _threads;
  Collection<CompletableFuture<Void>> _jobs;

  Collection<Consumes> _operands;
  Collection<OperationResult<Produces>> _results;

  /**
   * Creates a new parallel Task
   * 
   * @param executor Service to execute parallel jobs with
   * @param config   Config to associate with this task
   */
  protected ParallelTask(final @NonNull ExecutorService executor, final @NonNull Configuration config) {
    super(config);

    _executor = executor;
    _threads = new Semaphore(_taskConfig.threadParallelism);
    _jobs = new HashSet<>(_taskConfig.threadParallelism, 1f);

    _operands = ConcurrentHashMap.newKeySet(_taskConfig.collectionSize);
    _results = ConcurrentHashMap.newKeySet(_taskConfig.collectionSize);
  }

  /**
   * Dispatches a new completable job using this instance's executor, and returns
   * the job.
   * 
   * @param element Consumable unit of information
   * @return Completable job of work
   * @throws InterruptedException When interrupted while updating the number of
   *                              currently active threads.
   */
  private CompletableFuture<Void> dispatch(final @NonNull Consumes element) throws InterruptedException {
    _threads.acquire();
    CompletableFuture<Void> product = CompletableFuture
        .supplyAsync(() -> {
          _operands.add(element);
          OperationResult<Produces> result = operate(element);
          _results.add(result);

          return result;
        }, _executor)
        .thenAccept(result -> handlePost(element, result))
        .exceptionally(exception -> {
          handleFailure(element, exception);
          return null;
        });
    useMessage(String.format("Initialized dispatched job : %s", element), INFO);
    return product;
  }

  /**
   * Handles determining the state of an {@link #operate(Object) operation}.
   * 
   * @param operand Consumable unit of information
   * @param result  Produced unit of information
   */
  private void handlePost(final Consumes operand, final OperationResult<Produces> result) {
    PostResult currentStatus = post(result);
    useMessage(String.format("Posted job : %s", currentStatus), ERROR);

    switch (currentStatus) {
      case SUCCESS -> {
        _taskStats.successOccurrences++;

        _attempts.remove(operand);
        useMessage(String.format("Completed job : %s", operand), INFO);
      }

      case FAILURE_FATAL -> {
        _taskStats.failureFatalOccurrences++;

        _attempts.remove(operand);
        useMessage(String.format("Failed job : %s", operand), ERROR);
      }

      case FAILURE_RETRY -> {
        _taskStats.failureRetryOccurrences++;

        int attempt = _attempts.getOrDefault(operand, 0) + 1;

        if (attempt < _taskConfig.logicalRetries) {
          long delay = _taskConfig.retryScheduler.compute(attempt);

          _attempts.put(operand, attempt);
          _failed.add(new Locked<Consumes>(operand, delay, NANOSECONDS));

          useMessage(String.format("Queued job : %s", operand), DEBUG);
        } else {
          _attempts.remove(operand);
        }
      }
    }
    _threads.release();
  }

  /**
   * Handles determining the state of an {@link #operate(Object) operation}.
   * 
   * @param operand Consumable unit of information
   * @param cause   Cause for failure at any point during execution
   */
  private void handleFailure(final Consumes operand, Throwable cause) {
    int attempt = _attempts.getOrDefault(operand, 0) + 1;

    useMessage(String.format("Failed dispatched job : %s", operand), ERROR);
    if (attempt < _taskConfig.logicalRetries) {
      long delay = _taskConfig.retryScheduler.compute(attempt);

      _attempts.put(operand, attempt);
      _failed.add(new Locked<Consumes>(operand, delay, NANOSECONDS));

      useMessage(String.format("Queued dispatched job : %s", operand), DEBUG);
    }
    _threads.release();
  }

  @Override
  public void run() {
    useMessage(String.format("Operation started : %s", _taskConfig.name), DEBUG);
    while (!_completable.isDone()) {
      try {
        State state = _state.get();
        useMessage(String.format("Operation staged : %s", state), DEBUG);
        switch (state) {
          case CREATED -> {
            setup();
            useState(COLLECTING);
          }

          case AWAITING -> {
            await();
            useMessage(String.format("Awaited jobs : %d", _failed.size()), INFO);
            useState(COLLECTING);
          }

          case COLLECTING -> {
            Locked<Consumes> failed;
            int failedCount = 0;
            while ((failed = _failed.poll()) != null) {
              _collected.offer(failed.value);
              failedCount++;
            }
            useMessage(String.format("Added failed jobs : %d", failedCount), DEBUG);

            CollectionResult<Consumes> result = collect();
            switch (result) {
              case CollectionResult.Alive(List<Consumes> collection) -> {
                useMessage(String.format("Collection shape : Alive"), INFO);

                _collected.addAll(collection);
                _taskConfig.retryScheduler.reset();

                useMessage(String.format("Added collected jobs : %d", collection.size()), DEBUG);
              }

              case CollectionResult.Idle() -> {
                useMessage(String.format("Collection shape : Idle"), DEBUG);
              }

              case CollectionResult.Empty() -> {
                useMessage(String.format("Collection shape : Empty"), DEBUG);
              }
            }
            if (!_collected.isEmpty()) {
              setup();
              useState(DISPATCHING);
              return;
            }

            if (!_failed.isEmpty()) {
              useState(AWAITING);
              return;
            }

            useState(COMPLETED);
          }

          case DISPATCHING -> {
            _operands.clear();
            _results.clear();

            while (_threads.tryAcquire()) {
              Consumes element = _collected.poll();
              if (element == null) {
                _threads.release();
                break;
              }
              _jobs.add(dispatch(element));
            }
            CompletableFuture.allOf(_jobs.toArray(CompletableFuture[]::new)).thenRun(() -> {
              _taskStats.logicalCycleOccurrences++;

              _jobs.clear();
              useState(COLLECTING);
            });

            useState(WORKING);
          }

          case WORKING -> {
            // Do nothing, wait for jobs to complete ;)
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
            throw new IllegalStateException(String.format("Accessed invalid ParallelTask state : %s", state));
          }
        }
      } catch (final Throwable throwable) {
        useState(FAILED);
        useMessage(String.format("Operation interrupted : %s", throwable.getCause()), ERROR, throwable);
        _completable.completeExceptionally(throwable);
      }
      _taskStats.executiveCycleOccurrences++;
    }
    useMessage(String.format("Operation ended : %s", _taskConfig.name), DEBUG);
  }

  /**
   * Provides the most recent consumed operand
   * 
   * @return Previous consumed operand
   */
  public Collection<Consumes> getConsumed() {
    return _operands;
  }

  /**
   * Provides the most recent produced operand
   * 
   * @return Previous produced operand
   */
  public Collection<OperationResult<Produces>> getProduced() {
    return _results;
  }
}
