package com.github.jelatinone.model.graph;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;

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
    @NonNull ExecutionStage executionRef,

    @NonNull URL canonicalUrl,

    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;

  public TargetNode(UUID targetId, URL canonicalUrl, Instant emittedAt) {
    this(targetId, ExecutionStage.DISCOVERY, canonicalUrl, emittedAt);
  }

  public TargetNode(UUID targetId, ExecutionStage executionRef, URL canonicalUrl, Instant emittedAt) {
    this(SCHEMA_VERSION, targetId, executionRef, canonicalUrl, emittedAt);
  }
}
