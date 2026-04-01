package com.github.jelatinone.task;

import java.time.Instant;
import java.util.Map;

import com.github.jelatinone.models.investigate.Classification;

public record InvestigateState(
    Instant reviewedAt,
    Classification classification,
    double confidence,
    int discoveredTargetCount,
    boolean classificationResolved,
    boolean reusedRecentClassification) {

  public static InvestigateState initial(Instant reviewedAt) {
    return new InvestigateState(reviewedAt, new Classification(Map.of()), 0D, 0, false, false);
  }

  public InvestigateState withClassification(Classification nextClassification, double nextConfidence, boolean reused) {
    return new InvestigateState(
        reviewedAt,
        nextClassification,
        nextConfidence,
        discoveredTargetCount,
        true,
        reused);
  }

  public InvestigateState withDiscoveredTargetCount(int nextDiscoveredTargetCount) {
    return new InvestigateState(
        reviewedAt,
        classification,
        confidence,
        nextDiscoveredTargetCount,
        classificationResolved,
        reusedRecentClassification);
  }
}
