package com.github.jelatinone.task;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.investigate.InvestigateDocument;
import com.github.jelatinone.policy.RequestPolicyContext;
import com.github.jelatinone.task.policy.ClassificationConfiguration;

public record InvestigateContext(
		InvestigateDocument document,
		ClassificationConfiguration classificationConfiguration,
		AcquisitionService acquisitionService,
		Instant reviewedAt,
		InvestigateDocument retrievedInvestigate,
		ContentDocument retrievedContent,
		long envelopeSchemaVersion,
		ProcessingStage envelopeStage,
		UUID envelopeRequestId,
		UUID envelopeTargetId) implements RequestPolicyContext<InvestigateDocument> {
}
