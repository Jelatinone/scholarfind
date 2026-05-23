package com.github.jelatinone.task;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.model.classification.Category;
import com.github.jelatinone.model.classification.Classification;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Emission;

public record InvestigateState(
    Classification classification,

    boolean retrievedDocument,
    boolean resolvedClassification,
    int step,

    List<Emission<? extends Request<?>>> emissions) {

  public static InvestigateState initial() {
    return new InvestigateState(
        new Classification.Investigate(
            Map.of(),
            0D,
            Set.copyOf(EnumSet.allOf(Category.class)),
            Acquisition.Rank.INITIAL,
            Category.UNCLASSIFIED,
            false),
        false,
        false,
        0,
        List.of());
  }

  public InvestigateState withClassification(final Classification nextClassification) {
    return new InvestigateState(
        nextClassification,
        retrievedDocument(),
        resolvedClassification(),
        step(),
        emissions());
  }

  public InvestigateState withRetrievedDocument(final boolean nextRetrievedDocument) {
    return new InvestigateState(
        classification(),
        nextRetrievedDocument,
        resolvedClassification(),
        step(),
        emissions());
  }

  public InvestigateState withResolvedClassification(final boolean nextResolvedClassification) {
    return new InvestigateState(
        classification(),
        retrievedDocument(),
        nextResolvedClassification,
        step(),
        emissions());
  }

  public InvestigateState withStep(final int nextStep) {
    return new InvestigateState(
        classification(),
        retrievedDocument(),
        resolvedClassification(),
        nextStep,
        emissions());
  }

  public InvestigateState withStep() {
    return withStep(step() + 1);
  }

  public InvestigateState withEmissions(List<Emission<? extends Request<?>>> nextEmissions) {
    nextEmissions = List.copyOf(nextEmissions);
    return new InvestigateState(
        classification(),
        retrievedDocument(),
        resolvedClassification(),
        step(),
        nextEmissions);
  }

  public InvestigateState withEmissions(final Emission<? extends Request<?>> nextEmission) {
    List<Emission<? extends Request<?>>> nextEmissions = new ArrayList<>(emissions) {
      {
        add(nextEmission);
      }
    };
    return withEmissions(nextEmissions);
  }
}
