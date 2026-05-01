package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

/**
 * 
 * <h1>TargetEntity</h1>
 * 
 * Stable resolved identity corresponding to one or more equivalent
 * {@link TargetNode target nodes}.
 * 
 * @author Cody Washington
 */
public record EntityNode(
    long schemaVersion,

    @NonNull UUID entityId,
    @NonNull UUID reviewId,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;
}
