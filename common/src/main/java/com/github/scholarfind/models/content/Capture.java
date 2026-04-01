package com.github.scholarfind.models.content;

import com.github.scholarfind.models.shared.FetchReference;

public record Capture(
    FetchReference sourceSnapshot,
    FetchReference textSnapshot,
    ContentKind contentKind,
    ContentMediaType mediaType,
    CharacterEncoding characterEncoding,
    Long contentLength,
    String previewText) {
}
