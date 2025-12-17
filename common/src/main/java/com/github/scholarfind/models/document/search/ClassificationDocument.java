package com.github.scholarfind.models.document.search;

public record ClassificationDocument(
    ClassificationType type,
    Double confidence,
    ClassificationSource source) {
}
