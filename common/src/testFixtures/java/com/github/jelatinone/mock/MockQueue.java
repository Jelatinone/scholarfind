package com.github.jelatinone.mock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import com.github.jelatinone.api.Acknowledgement;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.QueueState;
import com.github.jelatinone.infra.queue.RetryableQueue;

public class MockQueue<Value> implements RetryableQueue<Value> {

  private final Deque<Value> input;
  private final Deque<Value> output;
  private final Deque<Value> retry;
  private final Deque<Value> error;
  private final QueueState drainedState;
  private boolean closed;

  public MockQueue() {
    this(QueueState.EMPTY, List.of());
  }

  public MockQueue(QueueState drainedState) {
    this(drainedState, List.of());
  }

  public MockQueue(QueueState drainedState, Collection<? extends Value> initialMessages) {
    this.input = new ArrayDeque<>(initialMessages);
    this.output = new ArrayDeque<>();
    this.retry = new ArrayDeque<>();
    this.error = new ArrayDeque<>();
    this.drainedState = drainedState;
  }

  public synchronized MockQueue<Value> addInput(Value message) {
    input.addLast(message);
    return this;
  }

  public synchronized MockQueue<Value> addInputs(Collection<? extends Value> messages) {
    messages.forEach(input::addLast);
    return this;
  }

  @Override
  public synchronized QueueResult<Value> poll(int messageCount) {
    int limit = Math.max(0, messageCount);
    List<QueueEnvelope<Value>> messages = new ArrayList<>(limit);

    while (limit-- > 0 && !input.isEmpty()) {
      Value message = input.removeFirst();
      messages.add(new QueueEnvelope<>(message, acknowledgement(message)));
    }

    QueueState state = !messages.isEmpty() || !input.isEmpty()
        ? QueueState.ACTIVE
        : drainedState;
    return new QueueResult<>(messages, state);
  }

  @Override
  public synchronized void send(Value message) {
    output.addLast(message);
  }

  @Override
  public synchronized void sendRetry(Value message) {
    retry.addLast(message);
  }

  @Override
  public synchronized void sendError(Value message) {
    error.addLast(message);
  }

  public synchronized List<Value> pendingMessages() {
    return List.copyOf(input);
  }

  public synchronized List<Value> sentMessages() {
    return List.copyOf(output);
  }

  public synchronized List<Value> retryMessages() {
    return List.copyOf(retry);
  }

  public synchronized List<Value> errorMessages() {
    return List.copyOf(error);
  }

  public synchronized boolean isClosed() {
    return closed;
  }

  @Override
  public synchronized void close() {
    closed = true;
  }

  private Acknowledgement acknowledgement(Value message) {
    return new Acknowledgement() {
      @Override
      public void success() {
      }

      @Override
      public void retry() {
        sendRetry(message);
      }

      @Override
      public void error() {
        sendError(message);
      }
    };
  }
}
