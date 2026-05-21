package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.github.jelatinone.api.graph.Edge;
import com.github.jelatinone.model.Schemable;

import lombok.NonNull;

/**
 * Directional relationship between graph nodes produced during review.
 * 
 * @author Cody Washington
 */
public sealed interface GraphEdge extends Schemable, Edge<UUID> permits GraphEdge.Parent, GraphEdge.Reducer {

  @NonNull
  UUID edgeId();

  @NonNull
  UUID reviewId();

  @NonNull
  UUID from();

  @NonNull
  UUID to();

  /**
   * A target resolved to a stable entity.
   */
  public record Reducer(
      long schemaVersion,

      @NonNull UUID edgeId,
      @NonNull UUID reviewId,

      @NonNull UUID targetId,
      @NonNull UUID entityId,

      @NonNull Instant emittedAt) implements GraphEdge {
    public static final long SCHEMA_VERSION = 1L;

    public Reducer(UUID edgeId, UUID reviewId, UUID targetId, UUID entityId, Instant emittedAt) {
      this(SCHEMA_VERSION, edgeId, reviewId, targetId, entityId, emittedAt);
    }

    @Override
    public @NonNull UUID from() {
      return targetId();
    }

    @Override
    public @NonNull UUID to() {
      return entityId();
    }

    @Override
    public @NonNull UUID canonicalId() {
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

      @NonNull UUID edgeId,
      @NonNull UUID reviewId,

      @NonNull UUID parentTargetId,
      @NonNull UUID childTargetId,

      @NonNull Instant emittedAt) implements GraphEdge {

    public static final long SCHEMA_VERSION = 1L;

    public Parent(UUID edgeId, UUID reviewId, UUID parentTargetId, UUID childTargetId, Instant emittedAt) {
      this(SCHEMA_VERSION, edgeId, reviewId, parentTargetId, childTargetId, emittedAt);
    }

    @Override
    public @NonNull UUID from() {
      return parentTargetId();
    }

    @Override
    public @NonNull UUID to() {
      return childTargetId();
    }

    @Override
    public @NonNull UUID canonicalId() {
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
