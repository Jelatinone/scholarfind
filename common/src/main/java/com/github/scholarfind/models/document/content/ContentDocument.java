package com.github.scholarfind.models.document.content;

import java.util.UUID;

public record ContentDocument(
    UUID id,
    String rawContent,
    String fingerprint) {
}
