package com.github.jelatinone.model.transit;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public record Letter<Content extends Request>(
    long schemaVersion,

    @NonNull UUID targetId,
    @NonNull UUID reviewId,

    @NonNull ExecutionStage executionRef,
    Content content,

    @NonNull Instant emittedAt) {
  public static final Long SCHEMA_VERSION = 1L;

  public Letter(UUID targetId, UUID reviewId, ExecutionStage executionRef, Content content, Instant emittedAt) {
    this(SCHEMA_VERSION, targetId, reviewId, executionRef, content, emittedAt);
  }
}
