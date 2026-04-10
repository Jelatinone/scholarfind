package com.github.jelatinone.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.policy.EmissionIntent;

public record InvestigateState(
		Instant reviewedAt,
		ClassificationStub classification,
		double confidence,
		int discoveredTargetCount,
		boolean classificationResolved,
		boolean reusedRecentClassification,
		List<EmissionIntent<? extends Request>> emissions) {

	public static InvestigateState initial(Instant reviewedAt) {
		return new InvestigateState(
				reviewedAt,
				new ClassificationStub(Map.of(), 0D, Set.of()),
				0D,
				0,
				false,
				false,
				List.of());
	}

	public InvestigateState withClassification(ClassificationStub nextClassification, double nextConfidence,
			boolean reused) {
		return new InvestigateState(
				reviewedAt,
				nextClassification,
				nextConfidence,
				discoveredTargetCount,
				true,
				reused,
				emissions);
	}

	public InvestigateState withDiscoveredTargetCount(int nextDiscoveredTargetCount) {
		return new InvestigateState(
				reviewedAt,
				classification,
				confidence,
				nextDiscoveredTargetCount,
				classificationResolved,
				reusedRecentClassification,
				emissions);
	}

	public InvestigateState withEmissions(List<EmissionIntent<? extends Request>> nextEmissions) {
		return new InvestigateState(
				reviewedAt,
				classification,
				confidence,
				discoveredTargetCount,
				classificationResolved,
				reusedRecentClassification,
				List.copyOf(nextEmissions));
	}

	public InvestigateState withEmission(EmissionIntent<? extends Request> nextEmission) {
		List<EmissionIntent<? extends Request>> nextPlannedEmissions = new ArrayList<>(emissions);
		nextPlannedEmissions.add(nextEmission);
		return withEmissions(nextPlannedEmissions);
	}
}
