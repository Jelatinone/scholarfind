package com.github.jelatinone.model.archive;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

public record ArchiveHeader(
    long schemaVersion,

    @NonNull UUID entityId,
    @NonNull UUID reviewId,

    @NonNull ArchiveState state,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public ArchiveHeader(UUID entityId, UUID reviewId, ArchiveState state, Instant emittedAt) {
    this(SCHEMA_VERSION, entityId, reviewId, state, emittedAt);
  }
}
