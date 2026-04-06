package com.github.jelatinone.acquisition;

import com.github.jelatinone.models.content.CharacterEncoding;
import com.github.jelatinone.models.content.ContentKind;
import com.github.jelatinone.models.content.ContentMediaType;

public record DetectedContent(
    ContentKind contentKind,
    ContentMediaType mediaType,
    CharacterEncoding characterEncoding) {
}
