package com.github.jelatinone.model.graph;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

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

		UUID targetId,
		URL canonicalUrl,

		Instant emittedAt) {
}
