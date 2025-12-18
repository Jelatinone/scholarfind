package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.State.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

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
  static Logger _logger = Logger.getLogger(ParallelTask.class.getName());

  ExecutorService _executor;
  Semaphore _concurrency;
  Collection<CompletableFuture<Void>> _dependencies;

  Map<Consumes, Integer> _attempts;
  Collection<Consumes> _failed;

  @NonFinal
  Collection<Consumes> collected;
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
    _failed = new HashSet<>();

    _attempts = new ConcurrentHashMap<>();

    operands = new HashSet<>();
    results = new HashSet<>();
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
  private synchronized void handlePost(final Consumes operand, final Produces result) {
    boolean ok = post(result);
    if (!ok) {
      int attempt = _attempts.getOrDefault(operand, 0) + 1;
      if (attempt < _options.operandRetires) {
        _attempts.put(operand, attempt);
        _failed.add(operand);
      }
    } else {
      _attempts.put(operand, 0);
    }
    _concurrency.release();
  }

  /**
   * Handles determining the state of an {@link #operate(Object) operation}.
   * 
   * @param operand Consumable unit of informaton
   * @param cause   Rease for failure at any point during execution
   */
  private synchronized void handleFailure(final Consumes operand, Throwable cause) {
    int attempt = _attempts.getOrDefault(operand, 0) + 1;
    if (attempt < _options.operandRetires) {
      _attempts.put(operand, attempt);
      _failed.add(operand);
    }
    _concurrency.release();
  }

  @Override
  public synchronized void run() {
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
                _failed.clear();

                collected.addAll(_failed);
                collected.addAll(collection);
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
            for (final Consumes element : collected) {
              CompletableFuture<Void> product = dispatch(element);
              _dependencies.add(product);
            }
            CompletableFuture
                .allOf(_dependencies.toArray(CompletableFuture[]::new))
                .join();
            useState(COLLECTING);
          }

          case RESTARTING -> {
            restart();
            _attempts.replaceAll((operand, attempt) -> 0);
            _failed.clear();
            useState(CREATED);
          }

          case COMPLETED, FAILED -> {
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
