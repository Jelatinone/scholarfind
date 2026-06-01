package com.github.jelatinone.model.publication;

import java.time.Instant;

import com.github.jelatinone.model.struct.Identity.EntityIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;

import lombok.NonNull;

public record PublicationHeader(
    long schemaVersion,

    @NonNull EntityIdentity entityId,
    @NonNull ReviewIdentity reviewId,

    @NonNull PublicationState state,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public PublicationHeader(EntityIdentity entityId, ReviewIdentity reviewId, PublicationState state,
      Instant emittedAt) {
    this(SCHEMA_VERSION, entityId, reviewId, state, emittedAt);
  }
}
