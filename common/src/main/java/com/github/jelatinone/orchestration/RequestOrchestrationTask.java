package com.github.jelatinone.orchestration;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;

import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.model.transit.RequestIntent;

import lombok.NonNull;

public final class RequestOrchestrationTask<R extends Request> implements Runnable, AutoCloseable {
  private final RequestIntentSource<R> source;
  private final Queue<Letter<R>> queue;
  private final ExecutionStage executionRef;
  private final Class<R> payloadType;
  private final int batchSize;
  private final Clock clock;

  public RequestOrchestrationTask(
      @NonNull RequestIntentSource<R> source,
      @NonNull Queue<Letter<R>> queue,
      @NonNull ExecutionStage executionRef,
      @NonNull Class<R> payloadType,
      int batchSize) {
    this(source, queue, executionRef, payloadType, batchSize, Clock.systemUTC());
  }

  public RequestOrchestrationTask(
      @NonNull RequestIntentSource<R> source,
      @NonNull Queue<Letter<R>> queue,
      @NonNull ExecutionStage executionRef,
      @NonNull Class<R> payloadType,
      int batchSize,
      @NonNull Clock clock) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be positive");
    }
    this.source = source;
    this.queue = queue;
    this.executionRef = executionRef;
    this.payloadType = payloadType;
    this.batchSize = batchSize;
    this.clock = clock;
  }

  @Override
  public void run() {
    Collection<RequestIntent<R>> intents = source.collectEligible(executionRef, batchSize);
    for (RequestIntent<R> intent : intents) {
      route(intent);
    }
  }

  private void route(RequestIntent<R> intent) {
    Letter<R> letter = intent.letter();
    try {
      validate(letter);
      queue.send(letter);
      source.markQueued(intent.intentId(), letter, Instant.now(clock));
    } catch (Throwable throwable) {
      source.markFailed(intent.intentId(), letter, throwable, Instant.now(clock));
    }
  }

  private void validate(Letter<R> letter) {
    if (letter.executionRef() != executionRef) {
      throw new IllegalArgumentException(String.format(
          "Intent for %s cannot be routed by %s orchestrator",
          letter.executionRef(),
          executionRef));
    }
    Request content = letter.content();
    if (content == null || !payloadType.isInstance(content)) {
      throw new IllegalArgumentException(String.format(
          "Intent payload type %s is not supported for %s stage",
          content == null ? "null" : content.getClass().getName(),
          executionRef));
    }
  }

  @Override
  public void close() throws Exception {
    try {
      source.close();
    } finally {
      queue.close();
    }
  }
}
