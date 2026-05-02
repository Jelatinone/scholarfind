package com.github.jelatinone.model.content;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

public record CaptureReference(
    @NonNull UUID targetId,

    @NonNull String storeKey,
    @NonNull String contentHash,

    @NonNull Instant emittedAt) {

  public CaptureReference(
      UUID targetId,
      String contentHash,
      Instant emittedAt) {
    this(targetId, "/", contentHash, emittedAt);
  }
}
