package com.github.jelatinone.models.content;

import com.github.jelatinone.models.shared.FetchReference;

public record Capture(
        FetchReference sourceSnapshot,
        FetchReference textSnapshot,
        ContentKind contentKind,
        ContentMediaType mediaType,
        CharacterEncoding characterEncoding,
        Long contentLength,
        String previewText) {
}
