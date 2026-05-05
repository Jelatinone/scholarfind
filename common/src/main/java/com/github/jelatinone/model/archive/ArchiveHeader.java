package com.github.jelatinone.model.archive;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;

import lombok.NonNull;

public record ArchiveHeader(
    long schemaVersion,

    @NonNull UUID entityId,
    @NonNull UUID reviewId,

    @NonNull ArchiveState state,

    @NonNull ExecutionStage emittedBy,
    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public ArchiveHeader(UUID entityId, UUID reviewId, ArchiveState state, ExecutionStage emittedBy, Instant emittedAt) {
    this(SCHEMA_VERSION, entityId, reviewId, state, emittedBy, emittedAt);
  }
}
