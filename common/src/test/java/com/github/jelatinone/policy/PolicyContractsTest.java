package com.github.jelatinone.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.policy.base.AttemptsPolicy;
import com.github.jelatinone.policy.base.DocumentSchemaPolicy;
import com.github.jelatinone.policy.base.ExpirationPolicy;
import com.github.jelatinone.policy.base.RequestSchemaPolicy;

class PolicyContractsTest {

  @SuppressWarnings("unchecked")
  @Test
  void policyPipeline_accumulatesState_andReturnsTerminalDecision() {
    PolicyPipeline<SimpleContext, Integer> pipeline = new PolicyPipeline<>(List.of(
        (context, state) -> new PolicyStep.Continue<>(state + 1),
        (context, state) -> new PolicyStep.Decide<>(
            new PolicyDecision.Drop<>(state + 1, PolicyReason.REQUEST_REJECTED, "stopped"))));
    InvestigateDocument document = StructTestFixtures.document(0, ExecutionStage.INVESTIGATE);

    PolicyDecision<Integer> decision = pipeline
        .process(context(document, StructTestFixtures.NOW), 1);

    PolicyDecision.Drop<Integer> drop = assertInstanceOf(PolicyDecision.Drop.class, decision);
    assertEquals(3, drop.state());
    assertEquals(PolicyReason.REQUEST_REJECTED, drop.reason());
  }

  @SuppressWarnings("unchecked")
  @Test
  void policyPipeline_returnsNextWhenNoPolicyTerminates() {
    PolicyPipeline<SimpleContext, Integer> pipeline = new PolicyPipeline<>(List.of(
        (context, state) -> new PolicyStep.Continue<>(state + 2),
        (context, state) -> new PolicyStep.Continue<>(state + 3)));
    InvestigateDocument document = StructTestFixtures.document(0, ExecutionStage.INVESTIGATE);

    PolicyDecision<Integer> decision = pipeline
        .process(context(document, StructTestFixtures.NOW), 1);

    PolicyDecision.Next<Integer> next = assertInstanceOf(PolicyDecision.Next.class, decision);
    assertEquals(6, next.state());
    assertTrue(next.emissions().isEmpty());
  }

  @Test
  void basePolicies_validateSchemasAttemptsAndExpiration() {
    InvestigateDocument document = StructTestFixtures.document(0, ExecutionStage.INVESTIGATE);
    SimpleContext context = context(document, StructTestFixtures.NOW);

    PolicyStep<Integer> continueStep = new DocumentSchemaPolicy<InvestigateRequest, InvestigateDocument, SimpleContext, Integer>(
        1L)
        .apply(context, 7);
    PolicyStep<Integer> attemptsDrop = new AttemptsPolicy<InvestigateRequest, InvestigateDocument, SimpleContext, Integer>(
        2)
        .apply(context(StructTestFixtures.document(2, ExecutionStage.INVESTIGATE), StructTestFixtures.NOW), 7);
    var staleHeader = new com.github.jelatinone.model.struct.DocumentHeader(
        document.documentHeader().schemaVersion(),
        document.documentHeader().targetId(),
        document.documentHeader().reviewId(),
        document.documentHeader().emittedBy(),
        Instant.parse("2026-05-01T00:00:00Z"));
    PolicyStep<Integer> expiredDrop = new ExpirationPolicy<InvestigateRequest, InvestigateDocument, SimpleContext, Integer>(
        Duration.ofDays(1))
        .apply(context(
            StructTestFixtures.document(0, ExecutionStage.INVESTIGATE)
                .withDocumentHeader(staleHeader),
            StructTestFixtures.NOW), 7);

    assertInstanceOf(PolicyStep.Continue.class, continueStep);
    assertEquals(PolicyReason.ATTEMPTS_EXCEEDED,
        assertInstanceOf(PolicyDecision.Drop.class, assertInstanceOf(PolicyStep.Decide.class, attemptsDrop).decision())
            .reason());
    assertEquals(PolicyReason.DOCUMENT_EXPIRED,
        assertInstanceOf(PolicyDecision.Drop.class, assertInstanceOf(PolicyStep.Decide.class, expiredDrop).decision())
            .reason());
  }

  @Test
  void expirationPolicy_usesContextReviewTime() {
    InvestigateDocument document = StructTestFixtures.document(0, ExecutionStage.INVESTIGATE);
    var header = new com.github.jelatinone.model.struct.DocumentHeader(
        document.documentHeader().schemaVersion(),
        document.documentHeader().targetId(),
        document.documentHeader().reviewId(),
        document.documentHeader().emittedBy(),
        Instant.parse("2026-05-01T00:00:00Z"));

    PolicyStep<Integer> step = new ExpirationPolicy<InvestigateRequest, InvestigateDocument, SimpleContext, Integer>(
        Duration.ofDays(1))
        .apply(context(
            document.withDocumentHeader(header),
            Instant.parse("2026-05-01T12:00:00Z")), 7);

    assertInstanceOf(PolicyStep.Continue.class, step);
  }

  @SuppressWarnings("unchecked")
  @Test
  void requestSchemaPolicy_rejectsMismatchedEnvelopeFields() {
    InvestigateDocument document = StructTestFixtures.document(0, ExecutionStage.INVESTIGATE);
    RequestSchemaPolicy<InvestigateRequest, InvestigateDocument, SimpleRequestContext, Integer> policy = new RequestSchemaPolicy<>(
        document.requestHeader().schemaVersion(),
        (state, reason, detail) -> new PolicyDecision.Drop<>(state, reason, detail));

    PolicyStep<Integer> continueStep = policy.apply(new SimpleRequestContext(
        1L,
        document.targetId(),
        document.reviewId(),
        StructTestFixtures.NOW,
        Optional.of(document),
        request(document)), 1);
    PolicyStep<Integer> mismatchStep = policy.apply(new SimpleRequestContext(
        99L,
        document.targetId(),
        UUID.randomUUID(),
        StructTestFixtures.NOW,
        Optional.of(document),
        request(document)), 1);
    PolicyStep<Integer> identityMismatchStep = policy.apply(new SimpleRequestContext(
        1L,
        document.targetId(),
        UUID.randomUUID(),
        StructTestFixtures.NOW,
        Optional.of(document),
        request(document)), 1);

    assertInstanceOf(PolicyStep.Continue.class, continueStep);
    PolicyDecision.Drop<Integer> drop = assertInstanceOf(
        PolicyDecision.Drop.class,
        assertInstanceOf(PolicyStep.Decide.class, mismatchStep).decision());
    assertEquals(PolicyReason.SCHEMA_MISMATCH, drop.reason());
    PolicyDecision.Drop<Integer> identityDrop = assertInstanceOf(
        PolicyDecision.Drop.class,
        assertInstanceOf(PolicyStep.Decide.class, identityMismatchStep).decision());
    assertEquals(PolicyReason.REQUEST_REJECTED, identityDrop.reason());
  }

  private static SimpleContext context(InvestigateDocument document, Instant reviewedAt) {
    return new SimpleContext(
        RequestHeader.SCHEMA_VERSION,
        document.targetId(),
        document.reviewId(),
        reviewedAt,
        Optional.of(document),
        request(document));
  }

  private static InvestigateRequest request(InvestigateDocument document) {
    return StructTestFixtures.request(document.requestHeader());
  }

  private record SimpleContext(
      long envelopeSchemaVersion,
      UUID envelopeTargetId,
      UUID envelopeReviewId,
      Instant envelopeReviewedAt,
      Optional<InvestigateDocument> retrievedDocument,
      InvestigateRequest receivedRequest) implements PolicyContext<InvestigateRequest, InvestigateDocument> {
  }

  private record SimpleRequestContext(
      long envelopeSchemaVersion,
      UUID envelopeTargetId,
      UUID envelopeReviewId,
      Instant envelopeReviewedAt,
      Optional<InvestigateDocument> retrievedDocument,
      InvestigateRequest receivedRequest) implements PolicyContext<InvestigateRequest, InvestigateDocument> {
  }
}
