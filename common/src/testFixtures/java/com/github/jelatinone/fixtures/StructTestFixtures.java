package com.github.jelatinone.fixtures;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;

public final class StructTestFixtures {

	public static final UUID TARGET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	public static final UUID REVIEW_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	public static final Instant NOW = Instant.parse("2026-05-07T12:00:00Z");

	private StructTestFixtures() {
	}

	public static RequestHeader requestHeader(int attempt, ExecutionStage emittedBy) {
		return new RequestHeader(TARGET_ID, REVIEW_ID, attempt, emittedBy, NOW);
	}

	public static DocumentHeader documentHeader(ExecutionStage emittedBy) {
		return new DocumentHeader(TARGET_ID, REVIEW_ID, emittedBy, NOW);
	}

	public static InvestigateRequest request(int attempt, ExecutionStage emittedBy) {
		return new InvestigateRequest(requestHeader(attempt, emittedBy), TARGET_ID, REVIEW_ID);
	}

	public static InvestigateDocument document(int attempt, ExecutionStage emittedBy) {
		return new InvestigateDocument(
				documentHeader(emittedBy),
				requestHeader(attempt, emittedBy),
				TARGET_ID,
				REVIEW_ID,
				classification(),
				Set.of(UUID.fromString("33333333-3333-3333-3333-333333333333")));
	}

	public static Classification.Interpreted classification() {
		return new Classification.Interpreted(
				Map.of(Category.LANDING, 0.95d),
				0.95d,
				Set.of(Category.LANDING),
				Category.LANDING,
				true);
	}

}
