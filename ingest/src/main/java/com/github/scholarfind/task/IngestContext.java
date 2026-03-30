package com.github.scholarfind.task;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.ingest.IngestDocument;
import com.github.scholarfind.models.ingest.TargetRecord;
import com.github.scholarfind.policy.PolicyContext;

public record IngestContext(
    IngestDocument document,
    Instant reviewedAt,
    TargetRecord targetRecord,
    int maxDepth,
    Duration rescheduleCooldown,
    long envelopeSchemaVersion,
    ProcessingStage envelopeStage,
    UUID envelopeRequestId,
    UUID envelopeTargetId) implements PolicyContext<IngestDocument> {
}
