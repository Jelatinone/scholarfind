package com.github.jelatinone.meta.archetype.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.Tests;
import com.github.jelatinone.meta.archetype.policy.PolicyResult;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyDecision;

class PipelineArchetypeContractsTest {

	@Test
	void processPipeline_persistsDocument_andBuildsEmissions() {
		TestPipelineArchetype archetype = new TestPipelineArchetype();
		InvestigateDocument document = Tests.document(0, ExecutionStage.INVESTIGATE);
		InvestigateRequest request = Tests.request(0, ExecutionStage.INVESTIGATE);
		Emission<InvestigateRequest> emission = new Emission<>(request, java.time.Duration.ZERO, ExecutionStage.ANNOTATE);
		PolicyResult<Letter<InvestigateRequest>, String, Integer> policy = new PolicyResult<>(
				Tests.letter(0, ExecutionStage.INVESTIGATE),
				"context",
				new PolicyDecision.Next<>(7, null, null, java.util.Set.of(emission)),
				Tests.NOW,
				Tests.NOW);
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
				Tests.letter(1, ExecutionStage.INVESTIGATE),
				Tests.document(1, ExecutionStage.INVESTIGATE),
				new PolicyDecision.Next<>(1),
				List.of(),
				Tests.NOW,
				Tests.NOW);

		Letter<InvestigateRequest> retry = archetype.retryEnvelope(output);

		assertEquals(2, retry.content().requestHeader().attempt());
		assertEquals(output.input(), archetype.errorEnvelope(output));
	}

	private static final class TestPipelineArchetype
			implements PipelineArchetype<InvestigateRequest, InvestigateRequest, String, Integer, InvestigateDocument> {
		private InvestigateDocument persistedDocument;
		private int persistCalls;

		@Override
		public PersistResult<Integer, InvestigateDocument> persistDocument(
				Letter<InvestigateRequest> input,
				String context,
				PolicyDecision.Next<Integer> decision,
				Instant occurredAt) {
			persistCalls++;
			return new PersistResult<>(persistedDocument, decision);
		}

		@Override
		public void persistExecution(Letter<InvestigateRequest> input, PolicyDecision<?> decision, Instant occurredAt) {
			persistCalls++;
		}

		@Override
		public void persistAttempt(
				Letter<InvestigateRequest> input,
				PolicyDecision<?> decision,
				Instant initializedAt,
				Instant occurredAt,
				Throwable throwable) {
		}

		@Override
		public InvestigateRequest buildRequest(InvestigateRequest request, RequestHeader header) {
			return new InvestigateRequest(header, request.targetId(), request.reviewId());
		}

		@Override
		public <Emit extends com.github.jelatinone.model.struct.Request> Letter<Emit> buildEnvelope(
				Emission<Emit> emission,
				Letter<InvestigateRequest> input,
				String context,
				InvestigateDocument document) {
			Emit request = emission.request();
			return new Letter<>(request.targetId(), request.reviewId(), emission.executionRef(), request, Tests.NOW);
		}

		@Override
		public Collection<Letter<? extends com.github.jelatinone.model.struct.Request>> buildEmissions(
				Letter<InvestigateRequest> input,
				String context,
				java.util.Set<Emission<? extends com.github.jelatinone.model.struct.Request>> emissions,
				InvestigateDocument document) {
			return PipelineArchetype.super.buildEmissions(input, context, emissions, document);
		}
	}
}
