package com.github.jelatinone.acquisition;

import java.net.URL;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;

import lombok.NonNull;

public sealed interface Acquisition {

	UUID targetId();

	UUID reviewId();

	URL resolvedUrl();

	public record Initial(
			@NonNull UUID targetId,
			@NonNull UUID reviewId,

			@NonNull URL canonicalUrl) implements Acquisition {

		@Override
		public URL resolvedUrl() {
			return canonicalUrl();
		}
	}

	public record Metadata(
			@NonNull UUID targetId,
			@NonNull UUID reviewId,

			@NonNull URL effectiveUrl,

			@NonNull MediaType mediaType,
			@NonNull MediaEncoding mediaEncoding,
			@NonNull MediaMetadata mediaMetadata,

			@NonNull Instant emittedAt) implements Acquisition {

		@Override
		public URL resolvedUrl() {
			return effectiveUrl();
		}
	}

	public record Interpreted(
			@NonNull UUID targetId,
			@NonNull UUID reviewId,

			@NonNull URL effectiveUrl,

			@NonNull MediaType mediaType,
			@NonNull MediaEncoding mediaEncoding,
			@NonNull MediaMetadata mediaMetadata,

			@NonNull Instant emittedAt,

			byte[] sourceBytes,
			Set<Projection> sourceProjections,

			@NonNull String sourceHash

	) implements Acquisition {

		public Interpreted(@NonNull UUID targetId,
				@NonNull UUID reviewId,

				@NonNull URL effectiveUrl,

				@NonNull MediaType mediaType,
				@NonNull MediaEncoding mediaEncoding,
				@NonNull MediaMetadata mediaMetadata,

				@NonNull Instant emittedAt,

				byte[] sourceBytes,

				@NonNull String sourceHash) {
			this(
					targetId,
					reviewId,
					effectiveUrl,
					mediaType,
					mediaEncoding,
					mediaMetadata,
					emittedAt,
					sourceBytes,
					null,
					sourceHash);
		}

		public Interpreted {
			sourceProjections = sourceProjections == null ? Set.of() : Set.copyOf(sourceProjections);
		}

		public Interpreted withProjection(@NonNull Projection sourceProjection) {
			Set<Projection> updatedProjections = new HashSet<>(sourceProjections());
			updatedProjections.add(sourceProjection);
			return new Interpreted(
					targetId(),
					reviewId(),
					effectiveUrl(),
					mediaType(),
					mediaEncoding(),
					mediaMetadata(),
					emittedAt(),
					sourceBytes(),
					updatedProjections,
					sourceHash());
		}

		@Override
		public URL resolvedUrl() {
			return effectiveUrl();
		}
	}
}
