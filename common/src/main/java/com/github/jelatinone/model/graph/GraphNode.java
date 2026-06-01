package com.github.jelatinone.model.graph;

import java.net.URL;
import java.time.Instant;
import java.util.Map;

import com.github.jelatinone.api.graph.Vertex;
import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.model.struct.Identity;
import com.github.jelatinone.model.struct.Identity.EntityIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.SemanticIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;
import com.github.jelatinone.utility.Canonical;

import lombok.NonNull;

/**
 * Node stored in the traversal graph.
 * 
 * @author Cody Washington
 */
public sealed interface GraphNode<Identifier extends Identity>
    extends Schemable<Identifier>, Vertex<Identifier> permits GraphNode.Target, GraphNode.Entity, GraphNode.Semantic {

  /**
   * A discovered URL target that can be processed by execution stages.
   */
  public record Target(
      long schemaVersion,

      @NonNull TargetIdentity targetId,
      @NonNull URL canonicalUrl,

      @NonNull Instant emittedAt) implements GraphNode<TargetIdentity> {
    public static final long SCHEMA_VERSION = 1L;

    public Target(TargetIdentity targetId, URL canonicalUrl, Instant emittedAt) {
      this(SCHEMA_VERSION, targetId, canonicalUrl, emittedAt);
    }

    public static Target create(URL value) {
      return create(value, Instant.now());
    }

    public static Target create(URL value, Instant emittedAt) {
      URL canonical = Canonical.canonicalizeURL(value);
      return new Target(
          TargetIdentity.create(canonical),
          canonical,
          emittedAt);
    }

    public static Target create(TargetIdentity targetId, URL canonicalUrl,
        Instant emittedAt) {
      return new Target(targetId, canonicalUrl, emittedAt);
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

      @NonNull Instant emittedAt) implements GraphNode<EntityIdentity> {
    public static final long SCHEMA_VERSION = 1L;

    public Entity(EntityIdentity entityId, Instant emittedAt) {
      this(SCHEMA_VERSION, entityId, emittedAt);
    }

    public static Entity create(EntityIdentity entityId) {
      return create(entityId, Instant.now());
    }

    public static Entity create(EntityIdentity entityId, Instant emittedAt) {
      return new Entity(entityId, emittedAt);
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
          "emittedAt", emittedAt());
    }

    @Override
    public @NonNull String vertexLabel() {
      return "entity";
    }
  }

  public record Semantic(
      long schemaVersion,

      @NonNull SemanticIdentity semanticId,
      @NonNull ReviewIdentity reviewId,

      String label,

      @NonNull Instant emittedAt) implements GraphNode<SemanticIdentity> {
    public static final long SCHEMA_VERSION = 1L;

    public Semantic(SemanticIdentity semanticId, ReviewIdentity reviewId, String label, Instant emittedAt) {
      this(SCHEMA_VERSION, semanticId, reviewId, label, emittedAt);
    }

    public static Semantic create(SemanticIdentity entityId, ReviewIdentity reviewId, String label) {
      return create(entityId, reviewId, label, Instant.now());
    }

    public static Semantic create(SemanticIdentity entityId, ReviewIdentity reviewId, String label, Instant emittedAt) {
      return new Semantic(entityId, reviewId, label, emittedAt);
    }

    @Override
    public @NonNull SemanticIdentity canonicalId() {
      return semanticId();
    }

    @Override
    public @NonNull SemanticIdentity vertexId() {
      return semanticId();
    }

    @Override
    public @NonNull Map<String, Object> properties() {
      return Map.of(
          "schemaVersion", schemaVersion(),
          "semanticId", semanticId(),
          "reviewId", reviewId(),
          "emittedAt", emittedAt());
    }

    @Override
    public @NonNull String vertexLabel() {
      return "entity";
    }

  }
}
