package com.github.jelatinone.acquisition;

import com.github.jelatinone.model.content.MediaType;

import lombok.NonNull;

public sealed interface Projection {

	@NonNull
	MediaType mediaType();

	public record Interpreted<Interprets>(
			@NonNull MediaType mediaType,
			@NonNull Interprets interpretedSource) implements Projection {
	}

	public record Normalized(
			@NonNull MediaType mediaType,
			@NonNull String normalizedSource) implements Projection {
	}

	public record Preview(
			@NonNull MediaType mediaType,
			@NonNull String previewSource,

			long previewLength,
			long sourceLength) implements Projection {
	}
}
