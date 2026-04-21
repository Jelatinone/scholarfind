package com.github.jelatinone.model.transit;

import java.time.Instant;
import java.util.UUID;

import org.jsoup.helper.HttpConnection.Request;

import com.github.jelatinone.model.audit.ExecutionStage;

public record Letter<Content extends Request>(
		long schemaVersion,

		UUID targetId,
		UUID reviewId,

		ExecutionStage executionRef,
		Content content,

		Instant emittedAt) {
}
