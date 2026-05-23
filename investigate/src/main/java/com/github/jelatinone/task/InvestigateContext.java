package com.github.jelatinone.task;

import com.github.jelatinone.policy.PolicyContext;

import java.time.Instant;
import java.util.Optional;

import com.github.jelatinone.infra.InvestigateInfrastructure;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record InvestigateContext(
    long envelopeSchemaVersion,
    TargetIdentity envelopeTargetId,
    ReviewIdentity envelopeReviewId,
    Instant envelopeReviewedAt,

    Optional<InvestigateDocument> retrievedDocument,
    InvestigateRequest receivedRequest,

    InvestigateInfrastructure infrastructure

) implements PolicyContext<InvestigateRequest, InvestigateDocument> {
}
