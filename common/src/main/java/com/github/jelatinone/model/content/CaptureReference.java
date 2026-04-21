package com.github.jelatinone.model.content;

import java.time.Instant;
import java.util.UUID;

public record CaptureReference(
		UUID targetId,

		String contentHash,

		Instant emittedAt) {

}
