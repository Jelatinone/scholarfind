package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.Map;

import com.github.jelatinone.api.graph.Edge;
import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.model.struct.Identity.EdgeIdentity;
import com.github.jelatinone.model.struct.Identity.EntityIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

/**
 * Directional relationship between graph nodes produced during review.
 * 
 * @author Cody Washington
 */
public sealed interface GraphEdge<From extends com.github.jelatinone.model.struct.Identity, To extends com.github.jelatinone.model.struct.Identity>
    extends Schemable<EdgeIdentity>, Edge<EdgeIdentity, From, To>
    permits GraphEdge.Parent, GraphEdge.Reducer {

  @NonNull
  EdgeIdentity edgeId();

  @NonNull
  ReviewIdentity reviewId();

  @NonNull
  From from();

  @NonNull
  To to();

  /**
   * A target resolved to a stable entity.
   */
  public record Reducer(
      long schemaVersion,

      @NonNull EdgeIdentity edgeId,
      @NonNull ReviewIdentity reviewId,

      @NonNull TargetIdentity targetId,
      @NonNull EntityIdentity entityId,

      @NonNull Instant emittedAt) implements GraphEdge<TargetIdentity, EntityIdentity> {
    public static final long SCHEMA_VERSION = 1L;

    public Reducer(EdgeIdentity edgeId, ReviewIdentity reviewId, TargetIdentity targetId, EntityIdentity entityId,
        Instant emittedAt) {
      this(SCHEMA_VERSION, edgeId, reviewId, targetId, entityId, emittedAt);
    }

    @Override
    public @NonNull TargetIdentity from() {
      return targetId();
    }

    @Override
    public @NonNull EntityIdentity to() {
      return entityId();
    }

    @Override
    public @NonNull EdgeIdentity canonicalId() {
      return edgeId();
    }

    @Override
    public @NonNull Map<String, Object> properties() {
      return Map.of(
          "schemaVersion", schemaVersion(),
          "edgeId", edgeId(),
          "reviewId", reviewId(),
          "targetId", targetId(),
          "entityId", entityId(),
          "emittedAt", emittedAt());
    }

    @Override
    public @NonNull String edgeLabel() {
      return "reducer";
    }
  }

  /**
   * A parent target discovered a child target.
   */
  public record Parent(
      long schemaVersion,

      @NonNull EdgeIdentity edgeId,
      @NonNull ReviewIdentity reviewId,

      @NonNull TargetIdentity parentTargetId,
      @NonNull TargetIdentity childTargetId,

      @NonNull Instant emittedAt) implements GraphEdge<TargetIdentity, TargetIdentity> {

    public static final long SCHEMA_VERSION = 1L;

    public Parent(EdgeIdentity edgeId, ReviewIdentity reviewId, TargetIdentity parentTargetId,
        TargetIdentity childTargetId, Instant emittedAt) {
      this(SCHEMA_VERSION, edgeId, reviewId, parentTargetId, childTargetId, emittedAt);
    }

    @Override
    public @NonNull TargetIdentity from() {
      return parentTargetId();
    }

    @Override
    public @NonNull TargetIdentity to() {
      return childTargetId();
    }

    @Override
    public @NonNull EdgeIdentity canonicalId() {
      return edgeId();
    }

    @Override
    public @NonNull Map<String, Object> properties() {
      return Map.of(
          "schemaVersion", schemaVersion(),
          "edgeId", edgeId(),
          "reviewId", reviewId(),
          "parentTargetId", parentTargetId(),
          "childTargetId", childTargetId(),
          "emittedAt", emittedAt());
    }

    @Override
    public @NonNull String edgeLabel() {
      return "parent";
    }
  }
}
