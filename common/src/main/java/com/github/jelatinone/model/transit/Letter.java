package com.github.jelatinone.model.transit;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;

public record Letter<Content extends Request>(
		long schemaVersion,

		UUID targetId,
		UUID reviewId,

		ExecutionStage executionRef,
		Content content,

		Instant emittedAt) {
}
