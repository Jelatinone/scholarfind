package com.github.jelatinone.task;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.policy.RequestPolicyContext;
import com.github.jelatinone.task.policy.ClassificationConfiguration;

public record InvestigateContext(
    InvestigateDocument document,
    TargetNode target,
    ClassificationConfiguration classificationConfiguration,
    AcquisitionService acquisitionService,
    Instant reviewedAt,
    InvestigateDocument retrievedInvestigate,
    ContentDocument retrievedContent,
    long envelopeSchemaVersion,
    ExecutionStage envelopeStage,
    UUID envelopeTargetId,
    UUID envelopeReviewId) implements RequestPolicyContext<InvestigateDocument> {
}
