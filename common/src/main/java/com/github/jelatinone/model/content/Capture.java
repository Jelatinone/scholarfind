package com.github.jelatinone.model.content;

import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

public record Capture(
		@NonNull UUID targetId,
		@NonNull UUID reviewId,

		byte[] sourceBytes,

		@NonNull Instant emittedAt) {

}
