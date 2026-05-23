package com.github.jelatinone.mock;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import com.github.jelatinone.api.Acknowledgement;
import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.QueueEnvelope;

public class MockQueue<Value> implements Queue<Void, Value, Criteria<Void>> {

  public static final Criteria<Void> ANY = Criteria.duration(Duration.ZERO);

  private final Deque<Value> input;
  private final Deque<Value> output;
  private final Deque<Value> retry;
  private final Deque<Value> error;
  private boolean closed;

  public MockQueue() {
    this(List.of());
  }

  public MockQueue(Collection<? extends Value> initialMessages) {
    this.input = new ArrayDeque<>(initialMessages);
    this.output = new ArrayDeque<>();
    this.retry = new ArrayDeque<>();
    this.error = new ArrayDeque<>();
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
  public synchronized boolean query(Exists<Criteria<Void>> query) {
    return query(new Count<>(query.criteria())) > 0;
  }

  @Override
  public synchronized long query(Count<Criteria<Void>> query) {
    return input.size();
  }

  @Override
  public synchronized Optional<QueueEnvelope<Value>> query(Singular<Criteria<Void>> query) {
    Value message = input.pollFirst();
    return message == null ? Optional.empty() : Optional.of(new QueueEnvelope<>(message, acknowledgement(message)));
  }

  @Override
  public synchronized Collection<QueueEnvelope<Value>> query(Several<Criteria<Void>> query) {
    int limit = query.limit();
    List<QueueEnvelope<Value>> messages = new ArrayList<>(limit);

    while (limit-- > 0 && !input.isEmpty()) {
      Value message = input.removeFirst();
      messages.add(new QueueEnvelope<>(message, acknowledgement(message)));
    }

    return messages;
  }

  @Override
  public synchronized void queue(Criteria<Void> criteria, Value message) {
    output.addLast(message);
  }

  public synchronized void retry(Value message) {
    retry.addLast(message);
  }

  public synchronized void error(Value message) {
    error.addLast(message);
  }

  public synchronized void send(Value message) {
    output.addLast(message);
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
        MockQueue.this.retry(message);
      }

      @Override
      public void error() {
        MockQueue.this.error(message);
      }
    };
  }
}
