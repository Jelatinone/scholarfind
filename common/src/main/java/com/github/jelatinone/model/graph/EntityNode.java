package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

/**
 * 
 * <h1>TargetEntity</h1>
 * 
 * Stable resolved identity corresponding to one or more equivalent
 * {@link TargetNode target nodes}.
 * 
 * @author Cody Washington
 */
public record EntityNode(
		long schemaVersion,

		UUID entityId,
		UUID reviewId,

		Instant emittedAt) {
}
