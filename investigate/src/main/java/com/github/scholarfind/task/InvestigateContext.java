package com.github.scholarfind.task;

import java.time.Instant;
import java.util.UUID;

import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.shared.ContentDocument;
import com.github.scholarfind.policy.RequestPolicyContext;
import com.github.scholarfind.task.policy.ClassificationConfiguration;

public record InvestigateContext(
                InvestigateDocument document,
                ClassificationConfiguration classificationConfiguration,
                Instant reviewedAt,
                InvestigateDocument retrievedInvestigate,
                ContentDocument retrievedContent,
                long envelopeSchemaVersion,
                ProcessingStage envelopeStage,
                UUID envelopeRequestId,
                UUID envelopeTargetId) implements RequestPolicyContext<InvestigateDocument> {
}
