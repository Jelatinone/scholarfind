package com.github.jelatinone.model.graph;

import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.github.jelatinone.api.graph.Vertex;
import com.github.jelatinone.model.Schemable;

import lombok.NonNull;

/**
 * Node stored in the traversal graph.
 * 
 * @author Cody Washington
 */
public sealed interface GraphNode extends Schemable, Vertex<UUID> permits GraphNode.Target, GraphNode.Entity {

  /**
   * A discovered URL target that can be processed by execution stages.
   */
  public record Target(
      long schemaVersion,

      @NonNull UUID targetId,
      @NonNull URL canonicalUrl,

      @NonNull Instant emittedAt) implements GraphNode {
    public static final long SCHEMA_VERSION = 1L;

    public Target(UUID targetId, URL canonicalUrl, Instant emittedAt) {
      this(SCHEMA_VERSION, targetId, canonicalUrl, emittedAt);
    }

    @Override
    public @NonNull UUID canonicalId() {
      return targetId();
    }

    @Override
    public @NonNull UUID vertexId() {
      return targetId();
    }

    @Override
    public @NonNull Map<String, Object> properties() {
      return Map.of(
          "schemaVersion", schemaVersion(),
          "targetId", targetId(),
          "canonicalUrl", canonicalUrl(),
          "emittedAt", emittedAt());
    }

    @Override
    public @NonNull String vertexLabel() {
      return "target";
    }
  }

  /**
   * A stable resolved identity corresponding to one or more targets.
   */
  public record Entity(
      long schemaVersion,

      @NonNull UUID entityId,
      @NonNull UUID reviewId,

      @NonNull Instant emittedAt) implements GraphNode {
    public static final long SCHEMA_VERSION = 1L;

    public Entity(UUID entityId, UUID reviewId, Instant emittedAt) {
      this(SCHEMA_VERSION, entityId, reviewId, emittedAt);
    }

    @Override
    public @NonNull UUID canonicalId() {
      return entityId();
    }

    @Override
    public @NonNull UUID vertexId() {
      return entityId();
    }

    @Override
    public @NonNull Map<String, Object> properties() {
      return Map.of(
          "schemaVersion", schemaVersion(),
          "entityId", entityId(),
          "reviewId", reviewId(),
          "emittedAt", emittedAt());
    }

    @Override
    public @NonNull String vertexLabel() {
      return "entity";
    }
  }
}
