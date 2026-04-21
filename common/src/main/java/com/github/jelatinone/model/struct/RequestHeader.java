package com.github.jelatinone.model.struct;

import java.time.Instant;
import java.util.UUID;

/**
 * 
 * <h1>RequestHeader</h1>
 * 
 * A request header correlates a {@link TargetNode node} with a given request.
 * 
 * @author Cody Washington
 */
public record RequestHeader(
		UUID requestId,
		UUID targetId,

		int attempt,

		String emittedBy,
		Instant emittedAt) {
}
