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
}
