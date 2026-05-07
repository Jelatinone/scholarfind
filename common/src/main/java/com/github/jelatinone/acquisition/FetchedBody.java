package com.github.jelatinone.acquisition;

import java.net.URL;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;

import lombok.NonNull;

public record FetchedBody(
		@NonNull URL effectiveUrl,

		byte[] sourceBytes,
		String sourceHash,

		@NonNull MediaType mediaType,
		@NonNull MediaEncoding mediaEncoding,
		@NonNull MediaMetadata mediaMetadata) {

}
