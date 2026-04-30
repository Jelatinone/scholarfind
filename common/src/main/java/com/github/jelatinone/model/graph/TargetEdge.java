package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

/**
 * 
 * <h1>TargetParentEdge</h1>
 * 
 * Directional child-parent relationship between two {@link TargetNode nodes}
 * within the traversal graph created by a {@link TargetReview review}.
 * 
 * @author Cody Washington
 */
public record TargetEdge(
		long schemaVersion,

		UUID edgeId,
		UUID reviewId,

		UUID parentTargetId,
		UUID childTargetId,

		Instant emittedAt) {

}
