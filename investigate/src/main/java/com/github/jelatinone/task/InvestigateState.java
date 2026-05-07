package com.github.jelatinone.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Emission;

public record InvestigateState(
    Instant reviewedAt,
    Classification.Collected classification,
    double confidence,
    int discoveredTargetCount,
    boolean classificationResolved,
    boolean reusedRecentClassification,
    List<Emission<? extends Request>> emissions) {

  public static InvestigateState initial(Instant reviewedAt) {
    return new InvestigateState(
        reviewedAt,
        new Classification.Collected(Map.of(), 0D, Set.copyOf(EnumSet.allOf(Category.class))),
        0D,
        0,
        false,
        false,
        List.of());
  }

  public InvestigateState withClassification(Classification.Collected nextClassification, double nextConfidence,
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

  public InvestigateState withEmissions(List<Emission<? extends Request>> nextEmissions) {
    return new InvestigateState(
        reviewedAt,
        classification,
        confidence,
        discoveredTargetCount,
        classificationResolved,
        reusedRecentClassification,
        List.copyOf(nextEmissions));
  }

  public InvestigateState withEmission(Emission<? extends Request> nextEmission) {
    List<Emission<? extends Request>> nextPlannedEmissions = new ArrayList<>(emissions);
    nextPlannedEmissions.add(nextEmission);
    return withEmissions(nextPlannedEmissions);
  }
}
