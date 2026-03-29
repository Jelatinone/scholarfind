package com.github.scholarfind.task;

import java.time.Duration;
import java.time.Instant;

import com.github.scholarfind.models.ingest.IngestDocument;
import com.github.scholarfind.policy.PolicyContext;

public record IngestContext(
    IngestDocument document,
    Instant reviewedAt,
    IngestDocument retrievedIngest,
    int maxDepth,
    Duration rescheduleCooldown) implements PolicyContext<IngestDocument> {
}
