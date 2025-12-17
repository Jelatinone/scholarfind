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

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public non-sealed abstract class SequentialTask<Consumes, Produces> extends Task<Consumes, Produces> {
  static Logger _logger = Logger.getLogger(SequentialTask.class.getName());

  @NonFinal
  ListIterator<Consumes> _iterator;
  AtomicBoolean _lastOk;
  AtomicInteger _attempt;

  /**
   * Creates a new abstract Task
   * 
   * @param name Name of the task to be created
   */
  public SequentialTask(final @NonNull String name) {
    super(name);
    _lastOk = new AtomicBoolean();
    _attempt = new AtomicInteger();
  }

  @Override
  public synchronized void run() {
    _iterator = null;
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
              case CollectionResult.Afloat(List<Consumes> collection) -> {
                _iterator = collection.listIterator();
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
            if (!_iterator.hasNext()) {
              useState(COLLECTING);
              break;
            }
            result = operate(operand = _iterator.next());
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
            if (currentAttempt >= DEFAULT_OPERAND_RETRIES) {
              _attempt.set(0);
              if (_iterator.hasNext()) {
                useState(OPERATING);
              } else {
                useState(COLLECTING);
              }
            } else {
              _iterator.previous();
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
}
