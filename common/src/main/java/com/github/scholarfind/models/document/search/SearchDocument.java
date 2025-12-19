package com.github.scholarfind.models.document.search;

import com.github.scholarfind.models.document.HeaderDocument;
import com.github.scholarfind.models.document.TraceDocument;

public record SearchDocument(
        HeaderDocument header,
        TraceDocument trace,
        ClassificationDocument classification,
        ReasonType reason) {
}
