package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.State.*;
import static java.util.concurrent.TimeUnit.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

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

  static Logger _logger = LoggerFactory.getLogger(ParallelTask.class);

  ExecutorService _executor;
  Semaphore _concurrency;
  Collection<CompletableFuture<Void>> _dependencies;

  @NonFinal
  Collection<Consumes> operands;
  @NonFinal
  Collection<Produces> results;

  /**
   * Creates a new parallel Task
   * 
   * @param name     Name of the task to be created
   * @param executor Executor to execute parallel jobs with
   */
  protected ParallelTask(final @NonNull String name, final @NonNull ExecutorService executor) {
    this(name, executor, Options.builder().build());
  }

  /**
   * Creates a new parallel Task
   * 
   * @param name     Name of the task to be created
   * @param executor Executor to execute parallel jobs with
   * @param options  Options to associate with this task
   */
  protected ParallelTask(final @NonNull String name, final @NonNull ExecutorService executor,
      final @NonNull Options options) {
    super(name, options);

    _executor = executor;
    _concurrency = new Semaphore(_options.threadParallelism);

    _dependencies = new HashSet<>();

    operands = ConcurrentHashMap.newKeySet(_options.collectionSize);
    results = ConcurrentHashMap.newKeySet(_options.collectionSize);
  }

  /**
   * Dispatches a new completable job using this instance's executor, and returns
   * the job.
   * 
   * @param element Consumable unit of informaton
   * @return Completeable job of work
   * @throws InterruptedException When interrupted while updating the number of
   *                              currently active threads.
   */
  private CompletableFuture<Void> dispatch(final Consumes element) throws InterruptedException {
    _concurrency.acquire();
    CompletableFuture<Void> product = CompletableFuture
        .supplyAsync(() -> {
          operands.add(element);
          Produces result = operate(element);
          results.add(result);

          return result;
        }, _executor)
        .thenAccept(result -> handlePost(element, result))
        .exceptionally(exception -> {
          handleFailure(element, exception);
          return null;
        });
    return product;
  }

  /**
   * Handles determining the state of an {@link #operate(Object) operation}.
   * 
   * @param operand Consumable unit of informaton
   * @param result  Produced unit of information
   */
  private void handlePost(final Consumes operand, final Produces result) {
    boolean currentStatus = post(result);
    if (!currentStatus) {
      int attempt = _attempts.getOrDefault(operand, 0) + 1;
      if (attempt < _options.operandRetires) {
        long delay = _retryScheduler.compute(attempt);
        _attempts.put(operand, attempt);
        _failed.add(new DelayedValue<Consumes>(operand, delay, NANOSECONDS));
      }
    } else {
      _attempts.remove(operand);
    }
    _concurrency.release();
  }

  /**
   * Handles determining the state of an {@link #operate(Object) operation}.
   * 
   * @param operand Consumable unit of informaton
   * @param cause   Rease for failure at any point during execution
   */
  private void handleFailure(final Consumes operand, Throwable cause) {
    int attempt = _attempts.getOrDefault(operand, 0) + 1;
    if (attempt < _options.operandRetires) {
      long delay = _retryScheduler.compute(attempt);
      _attempts.put(operand, attempt);
      _failed.add(new DelayedValue<Consumes>(operand, delay, NANOSECONDS));
    }
    _concurrency.release();
  }

  @Override
  public void run() {
    while (!_completable.isDone()) {
      try {
        State state = _state.get();
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
                _collected.addAll(collection);
                _collectScheduler.reset();

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

          case DISPATCHING -> {
            operands.clear();
            results.clear();

            while (_concurrency.tryAcquire()) {
              Consumes element = _collected.poll();
              if (element == null) {
                _concurrency.release();
                break;
              }
              _dependencies.add(dispatch(element));
            }
            CompletableFuture
                .allOf(_dependencies.toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                  _dependencies.clear();
                  useState(COLLECTING);
                });
            useState(WORKING);
          }

          case WORKING -> {
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
            throw new IllegalStateException("Accessed invalid state during ParallelTask execution.");
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
  public Collection<Consumes> getConsumed() {
    return operands;
  }

  /**
   * Provides the most recent produced operand
   * 
   * @return Previous produced operand
   */
  public Collection<Produces> getProduced() {
    return results;
  }
}
