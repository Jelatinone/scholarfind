package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

/**
 * 
 * <h1>TargetReview</h1>
 * 
 * Unit of graph traversal or evaluation on a given {@link TargetNode node}.
 * 
 * @author Cody Washington
 */
public record TargetReview(
		long schemaVersion,

		UUID reviewId,
		UUID targetId,

		UUID causeReviewId,
		UUID causeEdgeId,

		TargetCause causedBy,

		Instant emittedAt) {
}
