package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

/**
 * 
 * <h1>TargetEdge</h1>
 * 
 * Unit of child-parent relationship between two {@link TargetNode nodes} within
 * the graph created by a {@link TargetReview review}.
 * 
 * @author Cody Washington
 */
public record TargetEdge(
		long schemaVersion,

		UUID edgeId,
		UUID reviewId,

		UUID parentNodeId,
		UUID childNodeId,

		Instant emittedAt) {

}
