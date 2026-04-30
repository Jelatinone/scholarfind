package com.github.jelatinone.model.archive;

import java.time.Instant;
import java.util.UUID;

public record ArchiveHeader(
		long schemaVersion,

		UUID entityId,
		UUID reviewId,

		ArchiveState state,

		String emittedBy,
		Instant emittedAt) {
}
