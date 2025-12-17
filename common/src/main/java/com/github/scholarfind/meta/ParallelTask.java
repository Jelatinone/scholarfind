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

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public non-sealed abstract class ParallelTask<Consumes, Produces> extends Task<Consumes, Produces> {
  static Logger _logger = Logger.getLogger(ParallelTask.class.getName());

  ExecutorService _executor;
  Semaphore _concurrency;
  Collection<CompletableFuture<Void>> _dependencies;

  Map<Consumes, Integer> _attempts;
  Collection<Consumes> _failed;

  @NonFinal
  Collection<Consumes> _collected;

  /**
   * Creates a new abstract Task
   * 
   * @param name     Name of the task to be created
   * @param executor Executor to execute parallel jobs with
   */
  public ParallelTask(final @NonNull String name, final ExecutorService executor) {
    super(name);

    _executor = executor;
    _concurrency = new Semaphore(DEFAULT_PARALLELISM);

    _dependencies = new HashSet<>();
    _failed = new HashSet<>();

    _attempts = new ConcurrentHashMap<>();
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
    CompletableFuture<Void> product = CompletableFuture.supplyAsync(() -> operate(element), _executor)
        .thenAccept(result -> handlePost(operand, result))
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
      if (attempt < DEFAULT_OPERAND_RETRIES) {
        _attempts.put(operand, attempt);
        _failed.add(operand);
      }
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
    if (attempt < DEFAULT_OPERAND_RETRIES) {
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
              case CollectionResult.Afloat(List<Consumes> collection) -> {
                _failed.clear();

                _collected.addAll(_failed);
                _collected.addAll(collection);
                useState(OPERATING);
              }

              case CollectionResult.Alive() -> {
                useState(AWAITING);
              }

              case CollectionResult.Empty() -> {
                useState(COMPLETED);
              }
            }
          }

          case OPERATING -> {
            for (final Consumes element : _collected) {
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
}
