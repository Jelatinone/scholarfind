package com.github.jelatinone.model.content;

import java.net.URL;
import java.util.UUID;

public record Capture(
		UUID targetId,

		URL canonicalUrl,

		MediaType mediaType,
		MediaEncoding mediaEncoding,
		MediaMetadata mediaMetadata,

		CaptureReference sourceReference,
		CaptureReference interpretedReference) {
}
