package com.github.jelatinone.meta.archetype.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.meta.archetype.policy.PolicyResult;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.policy.PolicyDecision;

class PipelineArchetypeContractsTest {

	@Test
	void processPipeline_persistsDocument_andBuildsEmissions() {
		TestPipelineArchetype archetype = new TestPipelineArchetype();
		InvestigateDocument document = StructTestFixtures.document(0, ExecutionStage.INVESTIGATE);
		InvestigateRequest request = StructTestFixtures.request(0, ExecutionStage.INVESTIGATE);
		Emission<InvestigateRequest> emission = new Emission<>(request, java.time.Duration.ZERO, ExecutionStage.ANNOTATE);
		PolicyResult<InvestigateRequest, String, Integer> policy = new PolicyResult<>(
				StructTestFixtures.request(0, ExecutionStage.INVESTIGATE),
				"context",
				new PolicyDecision.Next<>(7, null, null, java.util.Set.of(emission)),
				StructTestFixtures.NOW,
				StructTestFixtures.NOW);
		archetype.persistedDocument = document;

		PipelineResult<InvestigateDocument, InvestigateRequest> result = archetype.processPipeline(policy);

		assertSame(document, result.document());
		assertEquals(1, result.emissions().size());
		assertEquals(2, archetype.persistCalls);
		assertInstanceOf(PolicyDecision.Next.class, result.decision());
	}

	@Test
	void retryEnvelope_incrementsAttempt_andErrorEnvelope_returnsOriginalInput() {
		TestPipelineArchetype archetype = new TestPipelineArchetype();
		PipelineResult<InvestigateDocument, InvestigateRequest> output = new PipelineResult<>(
				StructTestFixtures.request(1, ExecutionStage.INVESTIGATE),
				StructTestFixtures.document(1, ExecutionStage.INVESTIGATE),
				new PolicyDecision.Next<>(1),
				List.of(),
				StructTestFixtures.NOW,
				StructTestFixtures.NOW);

		InvestigateRequest retry = archetype.retryEnvelope(output);

		assertEquals(2, retry.requestHeader().attempt());
		assertEquals(output.input(), archetype.errorEnvelope(output));
	}

	@Test
	void processPipeline_preservesTerminalDecisionCause() {
		TestPipelineArchetype archetype = new TestPipelineArchetype();
		Throwable cause = new IllegalStateException("retry");
		PolicyResult<InvestigateRequest, String, Integer> policy = new PolicyResult<>(
				StructTestFixtures.request(0, ExecutionStage.INVESTIGATE),
				"context",
				new PolicyDecision.Retry<>(7, null, null, cause),
				StructTestFixtures.NOW,
				StructTestFixtures.NOW);

		archetype.processPipeline(policy);

		assertSame(cause, archetype.persistedAttemptCause);
	}

	private static final class TestPipelineArchetype
			implements PipelineArchetype<InvestigateRequest, InvestigateRequest, String, Integer, InvestigateDocument> {
		private InvestigateDocument persistedDocument;
		private Throwable persistedAttemptCause;
		private int persistCalls;

		@Override
		public PersistResult<Integer, InvestigateDocument> persistDocument(
				InvestigateRequest input,
				String context,
				PolicyDecision.Next<Integer> decision,
				Instant occurredAt) {
			persistCalls++;
			return new PersistResult<>(persistedDocument, decision);
		}

		@Override
		public void persistExecution(InvestigateRequest input, PolicyDecision<?> decision, Instant occurredAt) {
			persistCalls++;
		}

		@Override
		public void persistAttempt(
				InvestigateRequest input,
				PolicyDecision<?> decision,
				Instant initializedAt,
				Instant occurredAt,
				Throwable throwable) {
			persistedAttemptCause = throwable;
		}

		@Override
		public InvestigateRequest buildRequest(InvestigateRequest request, RequestHeader header) {
			return new InvestigateRequest(header, request.targetId(), request.reviewId());
		}

		@Override
		public <Emit extends Request<Emit>> Emit buildEnvelope(
				Emission<Emit> emission,
				InvestigateRequest input,
				String context,
				InvestigateDocument document) {
			return emission.request();
		}

		@Override
		public Collection<? extends Request<?>> buildEmissions(
				InvestigateRequest input,
				String context,
				java.util.Set<Emission<? extends Request<?>>> emissions,
				InvestigateDocument document) {
			return PipelineArchetype.super.buildEmissions(input, context, emissions, document);
		}
	}
}
