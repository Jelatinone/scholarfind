package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.Map;

import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

/**
 * 
 * <h1>GraphReview</h1>
 * 
 * Unit of graph traversal or evaluation on a given {@link GraphNode node}.
 * 
 * @author Cody Washington
 */
public record GraphReview(
    long schemaVersion,

    @NonNull ReviewIdentity reviewId,
    @NonNull TargetIdentity targetId,

    @NonNull GraphReviewCause causedBy,
    @NonNull GraphReviewState reviewState,

    @NonNull Instant emittedAt) implements Schemable<ReviewIdentity> {
  public static final long SCHEMA_VERSION = 1L;

  public GraphReview(ReviewIdentity reviewId, TargetIdentity targetId, GraphReviewCause causedBy,
      GraphReviewState reviewState,
      Instant emittedAt) {
    this(SCHEMA_VERSION, reviewId, targetId, causedBy, reviewState, emittedAt);
  }

  @Override
  public @NonNull ReviewIdentity canonicalId() {
    return reviewId();
  }

  @Override
  public @NonNull Map<String, Object> properties() {
    return Map.of(
        "schemaVersion", schemaVersion(),
        "reviewId", reviewId(),
        "targetId", targetId(),
        "causedBy", causedBy(),
        "reviewState", reviewState(),
        "emittedAt", emittedAt());
  }
}
