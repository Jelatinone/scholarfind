package com.github.jelatinone.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.Tests;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.InvestigateDocument;
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

		PolicyDecision<Integer> decision = pipeline
				.process(new SimpleContext(Tests.document(0, ExecutionStage.INVESTIGATE), Tests.NOW), 1);

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

		PolicyDecision<Integer> decision = pipeline
				.process(new SimpleContext(Tests.document(0, ExecutionStage.INVESTIGATE), Tests.NOW), 1);

		PolicyDecision.Next<Integer> next = assertInstanceOf(PolicyDecision.Next.class, decision);
		assertEquals(6, next.state());
		assertTrue(next.emissions().isEmpty());
	}

	@Test
	void basePolicies_validateSchemasAttemptsAndExpiration() {
		InvestigateDocument document = Tests.document(0, ExecutionStage.INVESTIGATE);
		SimpleContext context = new SimpleContext(document, Tests.NOW);

		PolicyStep<Integer> continueStep = new DocumentSchemaPolicy<InvestigateDocument, SimpleContext, Integer>(1L)
				.apply(context, 7);
		PolicyStep<Integer> attemptsDrop = new AttemptsPolicy<InvestigateDocument, SimpleContext, Integer>(2)
				.apply(new SimpleContext(Tests.document(2, ExecutionStage.INVESTIGATE), Tests.NOW), 7);
		var staleHeader = new com.github.jelatinone.model.struct.DocumentHeader(
				document.documentHeader().schemaVersion(),
				document.documentHeader().targetId(),
				document.documentHeader().reviewId(),
				document.documentHeader().emittedBy(),
				Instant.parse("2026-05-01T00:00:00Z"));
		PolicyStep<Integer> expiredDrop = new ExpirationPolicy<InvestigateDocument, SimpleContext, Integer>(1)
				.apply(new SimpleContext(
						Tests.document(0, ExecutionStage.INVESTIGATE)
								.withDocumentHeader(staleHeader),
						Tests.NOW), 7);

		assertInstanceOf(PolicyStep.Continue.class, continueStep);
		assertEquals(PolicyReason.ATTEMPTS_EXCEEDED,
				assertInstanceOf(PolicyDecision.Drop.class, assertInstanceOf(PolicyStep.Decide.class, attemptsDrop).decision())
						.reason());
		assertEquals(PolicyReason.DOCUMENT_EXPIRED,
				assertInstanceOf(PolicyDecision.Drop.class, assertInstanceOf(PolicyStep.Decide.class, expiredDrop).decision())
						.reason());
	}

	@SuppressWarnings("unchecked")
	@Test
	void requestSchemaPolicy_rejectsMismatchedEnvelopeFields() {
		InvestigateDocument document = Tests.document(0, ExecutionStage.INVESTIGATE);
		RequestSchemaPolicy<InvestigateDocument, SimpleRequestContext, Integer> policy = new RequestSchemaPolicy<>(
				document.requestHeader().schemaVersion(),
				ExecutionStage.INVESTIGATE,
				(state, reason, detail) -> new PolicyDecision.Drop<>(state, reason, detail));

		PolicyStep<Integer> continueStep = policy.apply(new SimpleRequestContext(
				document,
				Tests.NOW,
				1L,
				ExecutionStage.INVESTIGATE,
				document.targetId(),
				document.reviewId()), 1);
		PolicyStep<Integer> mismatchStep = policy.apply(new SimpleRequestContext(
				document,
				Tests.NOW,
				99L,
				ExecutionStage.INVESTIGATE,
				document.targetId(),
				UUID.randomUUID()), 1);

		assertInstanceOf(PolicyStep.Continue.class, continueStep);
		PolicyDecision.Drop<Integer> drop = assertInstanceOf(
				PolicyDecision.Drop.class,
				assertInstanceOf(PolicyStep.Decide.class, mismatchStep).decision());
		assertEquals(PolicyReason.SCHEMA_MISMATCH, drop.reason());
	}

	private record SimpleContext(
			InvestigateDocument document,
			Instant reviewedAt) implements PolicyContext<InvestigateDocument> {
	}

	private record SimpleRequestContext(
			InvestigateDocument document,
			Instant reviewedAt,
			long envelopeSchemaVersion,
			ExecutionStage envelopeStage,
			UUID envelopeTargetId,
			UUID envelopeReviewId) implements RequestPolicyContext<InvestigateDocument> {
	}
}
