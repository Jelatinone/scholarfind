package com.github.scholarfind.meta;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import static com.github.scholarfind.meta.State.*;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public sealed abstract class Task<@NonNull Consumes, @NonNull Produces> implements Runnable, AutoCloseable
    permits ParallelTask, SequentialTask {
  static Integer DEFAULT_OPERAND_RETRIES = 5;
  static Integer DEFAULT_COLLECTION_SIZE = 10;
  static Integer DEFAULT_PARALLELISM = 5;

  static Logger _logger = Logger.getLogger(Task.class.getName());

  String _name;

  AtomicReference<State> _state;

  @NonFinal
  CompletableFuture<Void> _completable;
  Collection<Runnable> _listeners;

  @NonFinal
  Consumes operand = null;
  @NonFinal
  Produces result = null;

  /**
   * Creates a new abstract Task
   * 
   * @param name Name of the task to be created
   */
  protected Task(final @NonNull String name) {
    _name = name;

    _state = new AtomicReference<State>(CREATED);

    _completable = new CompletableFuture<>();
    _listeners = new HashSet<>();
  }

  /**
   * Collects all consumable data into a single collection for
   * {@link #operate(Serializable) operation} to be performed on each element
   * within the collection.
   * 
   * @return Collection of consumable data
   */
  protected abstract @NonNull CollectionResult<@NonNull Consumes> collect();

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
  protected abstract boolean post(final Produces operand);

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
  protected abstract void restart() throws IOException;

  /**
   * Performs a waiting operation during {@link #run() operation} of this Task
   * 
   * @throws IOException When a critical failure has occurred while trying to
   *                     restart
   * 
   * @apiNote Called only during {@link #run() operation} of this Task when a
   *          dead collection has been recieved.
   * 
   */
  protected abstract void await() throws IOException;

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
  }

  /**
   * Modifies the current status message of this {@link #run() operation} of this
   * `Task`
   * with a descriptive message.
   * 
   * @param message Descriptive message of current operation of this Task
   * @param level   Level of logging to attribute to this message
   */
  public synchronized void useMessage(final @NonNull String message, final @NonNull Level level) {
    throw new UnsupportedOperationException("Method not yet implemented");
  }

  /**
   * Modifies (safely) the current state of this instance.
   * 
   * @param state New state of task
   * @throws IllegalStateException When modifications are made to a
   *                               {@link State#FAILED failed} or
   *                               {@link State#COMPLETED completed} Task
   */
  protected synchronized void useState(final @NonNull State state) throws IllegalStateException {
    final State currentState = _state.get();
    if (currentState == FAILED || currentState == COMPLETED) {
      _completable = new CompletableFuture<>();
    }
    if (state == COMPLETED || state == FAILED) {
      _completable.complete(null);
    }
    this._state.set(state);
    _logger.fine(String.format("%s [%s] :: State Update", getName(), state.name()));
  }

  /**
   * Provides the name of this instance.
   * 
   * @return Name of this task
   */
  public String getName() {
    return _name;
  }

  /**
   * Provides the state of this instance.
   * 
   * @return Current state of this task
   */
  public State getState() {
    return _state.get();
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
