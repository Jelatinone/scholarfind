package com.github.jelatinone.task.policy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.model.investigate.Classification;

record ClassificationSelection(
    Map<Category, Double> contributions,
    List<Category> contenders,
    Category dominantKind,
    boolean minimumConfidenceExceeded) {

  static ClassificationSelection resolve(
      Classification.Collected classification,
      ClassificationConfiguration configuration) {
    Map<Category, Double> contributions = classification == null
        ? Map.of()
        : classification.categoryEstimates();
    if (contributions.isEmpty()) {
      return new ClassificationSelection(
          Map.of(),
          List.of(),
          Category.UNCLASSIFIED,
          false);
    }

    double dominance = contributions.values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0D);
    List<Category> contenders = contributions.entrySet().stream()
        .filter(entry -> dominance - entry.getValue() <= configuration.dominanceConfiguration().dominanceEpsilon())
        .map(Map.Entry::getKey)
        .toList();
    boolean minimumConfidenceExceeded = contenders.stream()
        .anyMatch(contender -> contributions.getOrDefault(contender, 0D) >= configuration.dominanceConfiguration()
            .minimumConfidence());
    Category dominantKind = contenders.stream()
        .max(Comparator.comparingDouble(category -> contributions.getOrDefault(category, 0D)))
        .orElse(Category.UNCLASSIFIED);
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
