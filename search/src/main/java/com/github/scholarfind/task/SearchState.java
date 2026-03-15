package com.github.scholarfind.task;

import java.time.Instant;
import java.util.Map;

import com.github.scholarfind.models.investigate.Classification;

public record SearchState(
    Instant reviewedAt,
    Classification classification,
    double confidence,
    int discoveredTargetCount,
    boolean classificationResolved,
    boolean reusedRecentClassification) {

  public static SearchState initial(Instant reviewedAt) {
    return new SearchState(reviewedAt, new Classification(Map.of()), 0D, 0, false, false);
  }

  public SearchState withClassification(Classification nextClassification, double nextConfidence, boolean reused) {
    return new SearchState(
        reviewedAt,
        nextClassification,
        nextConfidence,
        discoveredTargetCount,
        true,
        reused);
  }

  public SearchState withDiscoveredTargetCount(int nextDiscoveredTargetCount) {
    return new SearchState(
        reviewedAt,
        classification,
        confidence,
        nextDiscoveredTargetCount,
        classificationResolved,
        reusedRecentClassification);
  }
}
