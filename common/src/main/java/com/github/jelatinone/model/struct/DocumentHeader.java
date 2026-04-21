package com.github.jelatinone.model.struct;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.graph.TargetReview;

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
		UUID targetId,
		UUID reviewId,

		long schemaVersion,

		String emittedBy,
		Instant emittedAt) {
}
