package com.github.scholarfind.models.document.content;

import com.github.scholarfind.models.document.HeaderDocument;
import com.github.scholarfind.models.document.TraceDocument;

public record ContentDocument(
        HeaderDocument header,
        TraceDocument trace,
        String rawContent,
        String fingerprint) {
}
