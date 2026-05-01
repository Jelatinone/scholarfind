package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

/**
 * 
 * <h1>TargetEntityMembership</h1>
 * 
 * Resolved membership mapping between a discovered {@link TargetNode target}
 * and a stable {@link EntityNode entity}.
 * 
 * @author Cody Washington
 */
public record EntityEdge(
		long schemaVersion,

		@NonNull UUID targetId,
		@NonNull UUID entityId,

		@NonNull UUID reviewId,
		@NonNull Instant emittedAt) {
}
