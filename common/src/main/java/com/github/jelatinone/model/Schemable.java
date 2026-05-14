package com.github.jelatinone.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.NonNull;

/**
 * Stable shape that can be projected into schema-aware stores.
 */
public interface Schemable {

  long schemaVersion();

  @NonNull
  UUID canonicalId();

  @NonNull
  Map<String, Object> properties();

  @NonNull
  Instant emittedAt();
}
