package com.github.jelatinone.task;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.ingest.IngestDocument;
import com.github.jelatinone.models.ingest.TargetRecord;
import com.github.jelatinone.policy.RequestPolicyContext;

public record IngestContext(
        IngestDocument document,
        Instant reviewedAt,
        TargetRecord targetRecord,
        int maxDepth,
        Duration rescheduleCooldown,
        long envelopeSchemaVersion,
        ProcessingStage envelopeStage,
        UUID envelopeRequestId,
        UUID envelopeTargetId) implements RequestPolicyContext<IngestDocument> {
}
