package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

/**
 * 
 * <h1>TargetParentEdge</h1>
 * 
 * Directional child-parent relationship between two {@link TargetNode nodes}
 * within the traversal graph created by a {@link TargetReview review}.
 * 
 * @author Cody Washington
 */
public record TargetEdge(
    long schemaVersion,

    @NonNull UUID edgeId,

    @NonNull UUID reviewId,

    @NonNull UUID parentTargetId,
    @NonNull UUID childTargetId,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public TargetEdge(UUID edgeId, UUID reviewId, UUID parentTargetId, UUID childTargetId, Instant emittedAt) {
    this(SCHEMA_VERSION, edgeId, reviewId, parentTargetId, childTargetId, emittedAt);
  }
}
