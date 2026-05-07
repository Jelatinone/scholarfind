package com.github.jelatinone.orchestration;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.model.transit.RequestIntent;

import lombok.NonNull;

public interface RequestIntentSource<R extends Request> extends AutoCloseable {

  Collection<RequestIntent<R>> collectEligible(
      @NonNull ExecutionStage executionRef,
      int limit);

  void markQueued(
      @NonNull UUID intentId,
      @NonNull Letter<R> letter,
      @NonNull Instant queuedAt);

  void markFailed(
      @NonNull UUID intentId,
      @NonNull Letter<R> letter,
      @NonNull Throwable throwable,
      @NonNull Instant failedAt);

  @Override
  default void close() throws Exception {
  }
}
