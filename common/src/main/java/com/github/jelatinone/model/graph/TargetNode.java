package com.github.jelatinone.model.graph;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

/**
 * 
 * <h1>TargetNode</h1>
 * 
 * Unit of canonical identity within the graph, representing a given target URL
 * that has been discovered.
 * 
 * @author Cody Washington
 */
public record TargetNode(
    long schemaVersion,

    @NonNull UUID targetId,
    @NonNull URL canonicalUrl,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public TargetNode(UUID targetId, URL canonicalUrl, Instant emittedAt) {
    this(SCHEMA_VERSION, targetId, canonicalUrl, emittedAt);
  }
}
