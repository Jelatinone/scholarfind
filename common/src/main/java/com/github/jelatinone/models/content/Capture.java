package com.github.jelatinone.models.content;

import java.net.URL;

import com.github.jelatinone.models.shared.FetchReference;

public record Capture(
        URL effectiveUrl,
        Integer statusCode,
        Integer redirectHopCount,
        Integer setCookieCount,
        FetchReference sourceSnapshot,
        FetchReference textSnapshot,
        ContentKind contentKind,
        ContentMediaType mediaType,
        CharacterEncoding characterEncoding,
        Long contentLength,
        String previewText) {
}
