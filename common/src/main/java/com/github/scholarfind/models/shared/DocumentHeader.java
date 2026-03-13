package com.github.scholarfind.models.shared;

import java.time.Instant;
import java.util.UUID;

public record DocumentHeader(
    long schemaVersion,
    UUID documentId,
    UUID requestId,
    UUID targetId,
    Instant createdAt) {
}
