package com.github.jelatinone.model.struct;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.graph.TargetNode;

import lombok.NonNull;

/**
 * 
 * <h1>RequestHeader</h1>
 * 
 * A request header correlates a {@link TargetNode node} with a given request.
 * 
 * @author Cody Washington
 */
public record RequestHeader(
		long schemaVersion,

		@NonNull UUID requestId,
		@NonNull UUID targetId,

		int attempt,

		@NonNull ExecutionStage emittedBy,
		@NonNull Instant emittedAt) {

	public static RequestHeader retry(@NonNull RequestHeader current, @NonNull Instant enqueuedAt) {
		return new RequestHeader(
				current.schemaVersion(),
				current.requestId(),
				current.targetId(),
				current.attempt() + 1,
				current.emittedBy(),
				enqueuedAt);
	}

	public static RequestHeader next(@NonNull RequestHeader current, @NonNull Instant enqueuedAt, long schemaVersion) {
		return new RequestHeader(
				schemaVersion,
				current.requestId(),
				current.targetId(),
				0,
				current.emittedBy(),
				enqueuedAt);
	}

}
