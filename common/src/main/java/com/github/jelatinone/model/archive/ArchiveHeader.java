package com.github.jelatinone.model.archive;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;

import lombok.NonNull;

public record ArchiveHeader(
		long schemaVersion,

		@NonNull UUID entityId,
		@NonNull UUID reviewId,

		@NonNull ArchiveState state,

		@NonNull ExecutionStage emittedBy,
		@NonNull Instant emittedAt) {
}
