package com.github.jelatinone.models.shared;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RequestHeader(
    long schemaVersion,
    UUID requestId,
    int attempt,
    String idempotencyKey,
    Instant enqueuedAt) {

  public static RequestHeader retry(RequestHeader current, Instant enqueuedAt) {
    Objects.requireNonNull(current, "current");
    Objects.requireNonNull(enqueuedAt, "enqueuedAt");
    return new RequestHeader(
        current.schemaVersion(),
        current.requestId(),
        current.attempt() + 1,
        current.idempotencyKey(),
        enqueuedAt);
  }

  public static RequestHeader next(RequestHeader current, long schemaVersion, Instant enqueuedAt) {
    Objects.requireNonNull(current, "current");
    Objects.requireNonNull(enqueuedAt, "enqueuedAt");
    return new RequestHeader(
        schemaVersion,
        current.requestId(),
        0,
        current.idempotencyKey(),
        enqueuedAt);
  }
}
