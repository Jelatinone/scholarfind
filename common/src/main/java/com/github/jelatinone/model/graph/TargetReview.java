package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

/**
 * 
 * <h1>TargetReview</h1>
 * 
 * Unit of graph traversal or evaluation on a given {@link TargetNode node}.
 * 
 * @author Cody Washington
 */
public record TargetReview(
    long schemaVersion,

    @NonNull UUID reviewId,
    @NonNull UUID targetId,

    @NonNull UUID causeReviewId,
    @NonNull UUID causeEdgeId,

    @NonNull TargetCause causedBy,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;
}
