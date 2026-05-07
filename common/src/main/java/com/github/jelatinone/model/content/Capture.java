package com.github.jelatinone.model.content;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

public sealed interface Capture {

	UUID targetId();

	UUID reviewId();

	URL effectiveUrl();

	MediaType mediaType();

	MediaEncoding mediaEncoding();

	MediaMetadata mediaMetadata();

	Instant emittedAt();

	public record Metadata(
			@NonNull UUID targetId,
			@NonNull UUID reviewId,

			@NonNull URL effectiveUrl,

			@NonNull MediaType mediaType,
			@NonNull MediaEncoding mediaEncoding,
			@NonNull MediaMetadata mediaMetadata,

			@NonNull Instant emittedAt) implements Capture {
	}

	public record Resolved(
			@NonNull UUID targetId,
			@NonNull UUID reviewId,

			@NonNull URL effectiveUrl,

			byte[] sourceBytes,
			@NonNull String sourceHash,

			@NonNull MediaType mediaType,
			@NonNull MediaEncoding mediaEncoding,
			@NonNull MediaMetadata mediaMetadata,

			@NonNull Instant emittedAt) implements Capture {

	}

}
