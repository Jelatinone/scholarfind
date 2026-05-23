package com.github.jelatinone.model.archive;

import java.time.Instant;

import com.github.jelatinone.model.struct.Identity.EntityIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;

import lombok.NonNull;

public record ArchiveHeader(
    long schemaVersion,

    @NonNull EntityIdentity entityId,
    @NonNull ReviewIdentity reviewId,

    @NonNull ArchiveState state,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public ArchiveHeader(EntityIdentity entityId, ReviewIdentity reviewId, ArchiveState state, Instant emittedAt) {
    this(SCHEMA_VERSION, entityId, reviewId, state, emittedAt);
  }
}
