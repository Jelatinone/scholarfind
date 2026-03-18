package com.github.scholarfind.task.policy;

import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.InvestigateContext;
import com.github.scholarfind.task.InvestigateState;

public final class RecentClassificationReusePolicy implements Policy<InvestigateContext, InvestigateState> {

  @Override
  public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
    InvestigateDocument retrieved = context.retrievedInvestigate();
    if (retrieved == null || retrieved.classification() == null || retrieved.reviewedAt() == null) {
      return new PolicyStep.Continue<>(state);
    }

    if (retrieved.documentHeader().schemaVersion() != InvestigateDocument.schemaVersion) {
      return new PolicyStep.Continue<>(state);
    }

    long reuseWindowSeconds = (long) context.classificationConfiguration().reuseWindowDays() * 24 * 60 * 60;
    if (retrieved.reviewedAt().isBefore(context.reviewedAt().minusSeconds(reuseWindowSeconds))) {
      return new PolicyStep.Continue<>(state);
    }

    Classification classification = retrieved.classification();
    double confidence = classification.contributions().values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0D);
    if (confidence < context.classificationConfiguration().dominanceConfiguration().minimumConfidence()) {
      return new PolicyStep.Continue<>(state);
    }

    InvestigateState nextState = state
        .withClassification(classification, confidence, true)
        .withDiscoveredTargetCount(retrieved.discoveredTargetCount());
    return new PolicyStep.Continue<>(nextState);
  }
}
