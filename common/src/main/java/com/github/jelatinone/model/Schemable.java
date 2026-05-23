package com.github.jelatinone.model;

import java.time.Instant;
import java.util.Map;

import com.github.jelatinone.model.struct.Identity;
import lombok.NonNull;

/**
 * Stable shape that can be projected into schema-aware stores.
 */
public interface Schemable<Identifier extends Identity> {

  long schemaVersion();

  @NonNull
  Identifier canonicalId();

  @NonNull
  Map<String, Object> properties();

  @NonNull
  Instant emittedAt();
}
