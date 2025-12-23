package com.github.scholarfind.meta;

import static com.github.scholarfind.meta.State.*;
import static org.slf4j.event.Level.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.github.scholarfind.backoff.BackoffScheduler;
import com.github.scholarfind.backoff.ExponentialBackoffScheduler;
import com.github.scholarfind.utility.DelayedValue;

import io.netty.util.internal.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/**
 * 
 * <h1>Task</h1>
 * 
 * <p>
 * A generic description of a Tak which operates on the smallest possible unit
 * of `consumes` and outputs a result `produces`.
 * </p>
 * 
 * <p>
 * Used to perform mass operations of similar type `consumes` on a collection of
 * consumable data.
 * For example, a task which scrapes all of the data from a website, then parses
 * each individual tag and converts it to a `String`.
 * </p>
 * 
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public sealed abstract class Task<@NonNull Consumes, @NonNull Produces> implements Runnable, AutoCloseable
    permits ParallelTask, SequentialTask {

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @Builder.Default
    String name = String.format("Task-[%d]", ThreadLocalRandom.current().nextLong());

    @Builder.Default
    Level logLevel = INFO;

    @Builder.Default
    Integer logicalRetries = 5;

    @Builder.Default
    Integer threadParallelism = 5;

    @Builder.Default
    Integer collectionSize = 10;

    @Builder.Default
    Long awaitBaseTimeout = 100L,
        awaitMaximumTimeout = 3500L,
        awaitBaseFactor = 3 / 2L;

    @Builder.Default
    Long retryBaseTimeout = 100L,
        retryMaximumTimeout = 3500L,
        retryBackoffFactor = 3 / 2L;
  }

  @FieldDefaults(level = AccessLevel.PUBLIC)
  public static final class Statistics {
    Integer failFatalCount = 0;

    Integer failRetryCount = 0;

    Integer successCount = 0;

    Integer retryCount = 0;
  }

  static Logger _logger = LoggerFactory.getLogger(Task.class);

  Configuration _taskConfig;
  Statistics _taskStats;

  AtomicReference<State> _state;
  BackoffScheduler _collectScheduler;
  BackoffScheduler _retryScheduler;

  Map<Consumes, Integer> _attempts;
  Queue<DelayedValue<Consumes>> _failed;
  Queue<Consumes> _collected;

  @NonFinal
  CompletableFuture<Void> _completable;
  Collection<Runnable> _listeners;

  /**
   * Creates a new abstract Task
   * 
   * @param name   Name of the task to be created
   * @param config Options to associate with this task
   */
  protected Task(final @NonNull Configuration config) {
    _taskConfig = config;
    _taskStats = new Statistics();

    _state = new AtomicReference<State>();
    _collectScheduler = new ExponentialBackoffScheduler(
        _taskConfig.awaitBaseTimeout,
        _taskConfig.awaitMaximumTimeout,
        _taskConfig.awaitBaseFactor);
    _retryScheduler = new ExponentialBackoffScheduler(
        _taskConfig.retryBaseTimeout,
        _taskConfig.retryMaximumTimeout,
        _taskConfig.retryBackoffFactor);

    _attempts = new ConcurrentHashMap<>();
    _failed = new DelayQueue<>();
    _collected = new ConcurrentLinkedQueue<>();

    _completable = new CompletableFuture<>();
    _listeners = new HashSet<>();

    useState(CREATED);
  }

  /**
   * Collects all consumable data into a single collection for
   * {@link #operate(Serializable) operation} to be performed on each element
   * within the collection.
   * 
   * @return Collection of consumable data
   */
  protected abstract @NonNull CollectResult<@NonNull Consumes> collect();

  /**
   * Performs an operation on `consumable` data and maps to a `producible` a
   * result.
   * 
   * @param operand Data to be mapped
   * @return Mapped result
   */
  protected abstract Produces operate(final @NonNull Consumes operand);

  /**
   * Self-callback function to determine the validity of the resulting data
   * 
   * @param operand Data to be checked
   * @return Mapped result
   */
  protected abstract @NonNull Post post(final Produces operand);

  /**
   * Restarts the current instance, performs necessary clean-up operations on this
   * instance before restarting.
   * 
   * @throws IOException When a critical failure has occurred while trying to
   *                     restart
   * 
   * @apiNote Called only during {@link #run() operation} of this Task when a
   *          {@link #useState(State) state modification} has occurred
   * 
   */
  protected void restart() throws IOException {
  }

  /**
   * Sets up the current instance for {@link #run() operation}, performing
   * necessary operations that need to occur during the creation phase and when a
   * new collection has been returned with valid work.
   * 
   * @throws IOException When a critical failure has occurred while trying to
   *                     setup
   */
  protected void setup() throws IOException {
  }

  /**
   * Shuts down the current instance for {@link #run() operation}, performing
   * necessary cleaning operations that need to occur during the completion or
   * failure phases.
   * 
   * @throws IOException When a critical failure has occurred while trying to
   *                     shutdown
   */
  protected void shutdown() throws IOException {
  }

  /**
   * Performs a waiting operation during {@link #run() operation} of this Task.
   * This operation should also handle the updating of the state.
   * 
   * @throws InterruptedException When this operation has been interrupted while
   *                              awaiting
   * 
   * @apiNote Called only during {@link #run() operation} of this Task when a
   *          dead collection has been received.
   * 
   */
  protected synchronized void await() throws InterruptedException {
    long backoff = _collectScheduler.compute();
    Thread.sleep(backoff);

    useMessage(String.format("Awaited milliseconds : %s", backoff), INFO);
  }

  /**
   * Provides the {@link CompletableFuture Future} of this instance
   * 
   * @apiNote This should be used to await the dependencies of the given task
   *          using {@link CompletableFuture#join() join()}.
   * 
   * @return Completable future of the current operation
   */
  public CompletableFuture<Void> completable() {
    return _completable;
  }

  /**
   * Adds a message update listener to this instance, which
   * {@link Runnable#run() updates} on
   * each call to {@link #setMessage(String) update message}.
   * 
   * @param listener Listener to add as listener
   */
  public synchronized void useListener(final @NonNull Runnable listener) {
    _listeners.add(listener);
    useMessage(String.format("Registered listener : %s", listener.getClass().getName()), DEBUG);
  }

  /**
   * Modifies the current status message of this {@link #run() operation} of this
   * `Task` with a descriptive message.
   * 
   * @param message   Descriptive message of current operation of this Task
   * @param level     Level of logging to attribute to this message
   * @param arguments Additional arguments to include in this log
   */
  public synchronized void useMessage(final @NonNull String message, final @NonNull Level level,
      final @NonNull Object... arguments) {
    _listeners.forEach(Runnable::run);
    _logger.atLevel(_taskConfig.logLevel.toInt() > level.toInt() ? _taskConfig.logLevel : level)
        .log(message, arguments);
  }

  /**
   * Modifies (safely) the current state of this instance.
   * 
   * @param state New state of task
   */
  protected synchronized void useState(final @NonNull State state) {
    final State currentState = _state.get();
    if (currentState == FAILED || currentState == COMPLETED) {
      switch (state) {
        case FAILED, COMPLETED -> {
        }
        default -> _completable = new CompletableFuture<>();
      }
    }
    if (state == COMPLETED || state == FAILED) {
      _completable.complete(null);
    }
    useMessage(String.format("State update : %s -> %s", _state, state), INFO);
    this._state.set(state);
  }
}
