package com.github.jelatinone.task.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jelatinone.models.annotate.AnnotateRequest;
import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.models.investigate.ClassificationKind;
import com.github.jelatinone.policy.EmissionIntent;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;

public final class InvestigateOutcomePolicy implements Policy<InvestigateContext, InvestigateState> {
  private final ClassificationConfiguration configuration;

  public InvestigateOutcomePolicy(ClassificationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
    ClassificationStub classification = state.classification();
    Map<ClassificationKind, Double> contributions = classification == null ? Map.of()
        : classification.contributions();
    if (contributions.isEmpty()) {
      return new PolicyStep.Decide<>(
          PolicyDecision.drop(state, PolicyReason.REQUEST_REJECTED,
              "Investigate classification produced no signals"));
    }

    double dominance = contributions.values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0D);
    List<ClassificationKind> contenders = contributions.entrySet().stream()
        .filter(entry -> dominance - entry.getValue() <= configuration.dominanceConfiguration()
            .dominanceEpsilon())
        .map(Map.Entry::getKey)
        .toList();
    boolean boundary = contenders.stream()
        .anyMatch(
            contender -> contributions.getOrDefault(contender, 0D) >= configuration.dominanceConfiguration()
                .minimumConfidence());

    if (boundary
        && !contenders.contains(ClassificationKind.LANDING)
        && !contenders.contains(ClassificationKind.NOT_APPLICABLE)) {
      AnnotateRequest request = new AnnotateRequest(
          context.document().requestHeader(),
          context.document().target(),
          classification);
      Set<com.github.jelatinone.models.shared.ReasonCode> reasonCodes = state.reusedRecentClassification()
          ? Set.of(PolicyReason.RECENT_RESULT_REUSED)
          : Set.of();
      return new PolicyStep.Decide<>(
          PolicyDecision.next(
              state,
              reasonCodes,
              "Investigate request forwarded to annotate",
              List.of(new EmissionIntent<>(
                  request,
                  null,
                  null,
                  context.document().requestHeader().idempotencyKey()))));
    }

    return new PolicyStep.Decide<>(
        PolicyDecision.drop(state, PolicyReason.REQUEST_REJECTED,
            "Investigate request did not satisfy forwarding gate"));
  }
}
