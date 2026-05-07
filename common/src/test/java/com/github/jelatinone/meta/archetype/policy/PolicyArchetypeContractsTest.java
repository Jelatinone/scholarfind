package com.github.jelatinone.meta.archetype.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.Tests;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;

class PolicyArchetypeContractsTest {

	@Test
	void policyArchetype_buildsContextAndDecision() {
		TestPolicyArchetype archetype = new TestPolicyArchetype();

		PolicyResult<com.github.jelatinone.model.transit.Letter<InvestigateRequest>, String, Integer> result = archetype
				.processPolicy(Tests.letter(0, ExecutionStage.INVESTIGATE));

		assertEquals("INVESTIGATE", result.context());
		assertEquals(12, assertInstanceOf(PolicyDecision.Next.class, result.decision()).state());
	}

	@Test
	void policyOperationAndPersist_delegate_toArchetypeAndDisposition() {
		TestPolicyArchetype archetype = new TestPolicyArchetype();
		PolicyOperation<InvestigateRequest, String, Integer> operation = new PolicyOperation<>(archetype);
		List<String> callbacks = new ArrayList<>();
		PolicyPersist<com.github.jelatinone.model.transit.Letter<InvestigateRequest>, String, Integer> persist = new PolicyPersist<>(
				new PolicyDisposition<>() {
					@Override
					public void next(
							PolicyResult<com.github.jelatinone.model.transit.Letter<InvestigateRequest>, String, Integer> result) {
						callbacks.add("next");
					}

					@Override
					public void drop(
							PolicyResult<com.github.jelatinone.model.transit.Letter<InvestigateRequest>, String, Integer> result) {
						callbacks.add("drop");
					}

					@Override
					public void retry(
							PolicyResult<com.github.jelatinone.model.transit.Letter<InvestigateRequest>, String, Integer> result) {
						callbacks.add("retry");
					}

					@Override
					public void error(
							PolicyResult<com.github.jelatinone.model.transit.Letter<InvestigateRequest>, String, Integer> result) {
						callbacks.add("error");
					}
				});

		var result = operation.operate(Tests.letter(0, ExecutionStage.INVESTIGATE));

		assertInstanceOf(com.github.jelatinone.meta.result.PostResult.Success.class, persist.post(result));
		assertEquals(List.of("next"), callbacks);
		assertEquals(PolicyReason.OPERATION_EXCEPTION,
				assertInstanceOf(PolicyDecision.Error.class,
						archetype.recoverPolicy(Tests.letter(0, ExecutionStage.INVESTIGATE), new IllegalStateException("boom"))
								.decision())
						.reason());
	}

	private static final class TestPolicyArchetype implements PolicyArchetype<InvestigateRequest, String, Integer> {
		@Override
		public Integer buildState(String context) {
			return context.length();
		}

		@Override
		public String buildContext(com.github.jelatinone.model.transit.Letter<InvestigateRequest> input,
				Instant initializedAt) {
			return input.executionRef().name();
		}

		@Override
		public PolicyDecision<Integer> pipeline(String context, Integer state) {
			return new PolicyDecision.Next<>(state + 1);
		}
	}
}
