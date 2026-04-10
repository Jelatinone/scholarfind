package com.github.jelatinone.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jelatinone.models.shared.ReasonCode;
import com.github.jelatinone.models.shared.Request;

public record PolicyDecision<State>(
		State state,
		StageOutcome outcome,
		Set<ReasonCode> reasonCodes,
		String reasonDetail,
		RetryDirective retryDirective,
		List<EmissionIntent<? extends Request>> emissionIntents,
		Map<String, String> auditAttributes) {

	public PolicyDecision {
		reasonCodes = Set.copyOf(reasonCodes);
		emissionIntents = List.copyOf(emissionIntents);
		auditAttributes = Map.copyOf(auditAttributes);
	}

	public static <State> PolicyDecision<State> next(State state) {
		return new PolicyDecision<>(state, StageOutcome.NEXT, Set.of(), null, null, List.of(), Map.of());
	}

	public static <State> PolicyDecision<State> next(
			State state,
			Set<ReasonCode> reasonCodes,
			String reasonDetail,
			List<EmissionIntent<? extends Request>> emissionIntents) {
		return new PolicyDecision<>(state, StageOutcome.NEXT, reasonCodes, reasonDetail, null, emissionIntents, Map.of());
	}

	public static <State> PolicyDecision<State> drop(
			State state,
			ReasonCode reasonCode,
			String reasonDetail) {
		return new PolicyDecision<>(state, StageOutcome.DROP, Set.of(reasonCode), reasonDetail, null, List.of(), Map.of());
	}

	public static <State> PolicyDecision<State> retry(
			State state,
			ReasonCode reasonCode,
			String reasonDetail,
			RetryDirective retryDirective) {
		return new PolicyDecision<>(state, StageOutcome.RETRY, Set.of(reasonCode), reasonDetail, retryDirective, List.of(),
				Map.of());
	}

	public static <State> PolicyDecision<State> error(
			State state,
			ReasonCode reasonCode,
			String reasonDetail) {
		return new PolicyDecision<>(state, StageOutcome.ERROR, Set.of(reasonCode), reasonDetail, null, List.of(), Map.of());
	}

	public ReasonCode primaryReasonCode() {
		return reasonCodes.stream().findFirst().orElse(null);
	}
}
