package com.github.scholarfind.models.document.search;

import com.github.scholarfind.models.document.HeaderDocument;

public record SearchDocument(
        Long schemaVersion,
        HeaderDocument header,
        ClassificationDocument classification,
        ReasonType reason) {
}
