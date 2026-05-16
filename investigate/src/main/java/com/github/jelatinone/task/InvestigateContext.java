package com.github.jelatinone.task;

import com.github.jelatinone.policy.PolicyContext;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.infra.InvestigateInfrastructure;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;

public record InvestigateContext(
		long envelopeSchemaVersion,
		UUID envelopeTargetId,
		UUID envelopeReviewId,
		Instant envelopeReviewedAt,

		InvestigateDocument retrievedDocument,
		InvestigateRequest receivedRequest,

		InvestigateInfrastructure infrastructure

) implements PolicyContext<InvestigateRequest, InvestigateDocument> {
}
