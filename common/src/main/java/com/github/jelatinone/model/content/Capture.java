package com.github.jelatinone.model.content;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import lombok.NonNull;

public record Capture(
    @NonNull UUID targetId,

    @NonNull URL canonicalUrl,

    @NonNull MediaType mediaType,
    @NonNull MediaEncoding mediaEncoding,
    @NonNull MediaMetadata mediaMetadata,

    CaptureReference sourceCapture,
    CaptureReference interpretedCapture,

    @NonNull Instant emittedAt) {
}
