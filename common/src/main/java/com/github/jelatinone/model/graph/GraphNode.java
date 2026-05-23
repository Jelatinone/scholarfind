package com.github.jelatinone.model.graph;

import java.net.URL;
import java.time.Instant;
import java.util.Map;

import com.github.jelatinone.api.graph.Vertex;
import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.model.struct.Identity.DomainIdentity;
import com.github.jelatinone.model.struct.Identity.EntityIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

/**
 * Node stored in the traversal graph.
 * 
 * @author Cody Washington
 */
public sealed interface GraphNode<Identifier extends com.github.jelatinone.model.struct.Identity>
    extends Schemable<Identifier>, Vertex<Identifier>
    permits GraphNode.Target, GraphNode.Entity {

  /**
   * A discovered URL target that can be processed by execution stages.
   */
  public record Target(
      long schemaVersion,

      @NonNull DomainIdentity domainId,
      @NonNull TargetIdentity targetId,

      @NonNull URL canonicalUrl,

      @NonNull Instant emittedAt) implements GraphNode<TargetIdentity> {
    public static final long SCHEMA_VERSION = 1L;

    public Target(DomainIdentity domainId, TargetIdentity targetId, URL canonicalUrl, Instant emittedAt) {
      this(SCHEMA_VERSION, domainId, targetId, canonicalUrl, emittedAt);
    }

    @Override
    public @NonNull TargetIdentity canonicalId() {
      return targetId();
    }

    @Override
    public @NonNull TargetIdentity vertexId() {
      return targetId();
    }

    @Override
    public @NonNull Map<String, Object> properties() {
      return Map.of(
          "schemaVersion", schemaVersion(),
          "domainId", domainId(),
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

      @NonNull EntityIdentity entityId,
      @NonNull ReviewIdentity reviewId,

      @NonNull Instant emittedAt) implements GraphNode<EntityIdentity> {
    public static final long SCHEMA_VERSION = 1L;

    public Entity(EntityIdentity entityId, ReviewIdentity reviewId, Instant emittedAt) {
      this(SCHEMA_VERSION, entityId, reviewId, emittedAt);
    }

    @Override
    public @NonNull EntityIdentity canonicalId() {
      return entityId();
    }

    @Override
    public @NonNull EntityIdentity vertexId() {
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
