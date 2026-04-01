package com.github.jelatinone.task.policy;

import com.github.jelatinone.models.investigate.Classification;
import com.github.jelatinone.models.investigate.InvestigateDocument;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;

public final class ClassificationReusePolicy implements Policy<InvestigateContext, InvestigateState> {

  @Override
  public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
    InvestigateDocument retrieved = context.retrievedInvestigate();
    if (retrieved == null || retrieved.classification() == null || retrieved.reviewedAt() == null) {
      return new PolicyStep.Continue<>(state);
    }

    if (retrieved.documentHeader().schemaVersion() != InvestigateDocument.SCHEMA_VERSION) {
      return new PolicyStep.Continue<>(state);
    }

    if (retrieved.reviewedAt()
        .isBefore(context.reviewedAt().minus(context.classificationConfiguration().reuseWindowDays()))) {
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
