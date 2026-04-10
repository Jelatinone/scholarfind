package com.github.jelatinone.task.policy;

import java.util.List;
import java.util.Map;

import com.github.jelatinone.models.investigate.ClassificationKind;
import com.github.jelatinone.models.investigate.ClassificationStub;

record ClassificationSelection(
		Map<ClassificationKind, Double> contributions,
		List<ClassificationKind> contenders,
		ClassificationKind dominantKind,
		boolean minimumConfidenceExceeded) {

	static ClassificationSelection resolve(
			ClassificationStub classification,
			ClassificationConfiguration configuration) {
		Map<ClassificationKind, Double> contributions = classification == null
				? Map.of()
				: classification.contributions();
		if (contributions.isEmpty()) {
			return new ClassificationSelection(
					Map.of(),
					List.of(),
					ClassificationKind.UNCLASSIFIED,
					false);
		}

		double dominance = contributions.values().stream()
				.mapToDouble(Double::doubleValue)
				.max()
				.orElse(0D);
		List<ClassificationKind> contenders = contributions.entrySet().stream()
				.filter(entry -> dominance - entry.getValue() <= configuration.dominanceConfiguration().dominanceEpsilon())
				.map(Map.Entry::getKey)
				.toList();
		boolean minimumConfidenceExceeded = contenders.stream()
				.anyMatch(contender -> contributions.getOrDefault(contender, 0D) >= configuration.dominanceConfiguration()
						.minimumConfidence());
		ClassificationKind dominantKind = contenders.stream()
				.reduce(ClassificationKind::max)
				.orElse(ClassificationKind.UNCLASSIFIED);
		return new ClassificationSelection(
				Map.copyOf(contributions),
				contenders,
				dominantKind,
				minimumConfidenceExceeded);
	}

	boolean hasSignals() {
		return !contributions.isEmpty();
	}
}