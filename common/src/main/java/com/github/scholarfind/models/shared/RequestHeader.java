package com.github.scholarfind.models.shared;

import java.time.Instant;
import java.util.UUID;

public record RequestHeader(
    long schemaVersion,
    UUID requestId,
    int attempt,
    String idempotencyKey,
    Instant enqueuedAt) {
}
