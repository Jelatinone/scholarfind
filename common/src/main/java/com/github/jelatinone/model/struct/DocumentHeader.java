package com.github.jelatinone.model.struct;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.graph.TargetReview;

import lombok.NonNull;

/**
 * 
 * <h1>DocumentHeader</h1>
 * 
 * A document represents the final, persisted state of a given stage operation,
 * an audit of the stage state at the time of {@link TargetReview review}
 * processing.
 * 
 * A document header correlates a {@link TargetNode node}, a document, and
 * {@link RequestHeader request} made for a stage operation to be made on a
 * given target.
 * 
 * @author Cody Washington
 * 
 */
public record DocumentHeader(
    long schemaVersion,

    @NonNull UUID targetId,
    @NonNull UUID reviewId,

    @NonNull ExecutionStage emittedBy,
    @NonNull Instant emittedAt) {
  public static final long SCHEMA_VERSION = 1L;
}
