package com.github.scholarfind.task.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.scholarfind.models.annotate.AnnotateRequest;
import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.ClassificationType;
import com.github.scholarfind.policy.EmissionIntent;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyReason;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.SearchContext;
import com.github.scholarfind.task.SearchState;

public final class SearchOutcomePolicy implements Policy<SearchContext, SearchState> {
  private final ClassificationConfiguration configuration;

  public SearchOutcomePolicy(ClassificationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public PolicyStep<SearchState> apply(SearchContext context, SearchState state) {
    Classification classification = state.classification();
    Map<ClassificationType, Double> contributions = classification == null ? Map.of() : classification.contributions();
    if (contributions.isEmpty()) {
      return new PolicyStep.Decide<>(
          PolicyDecision.drop(state, PolicyReason.REQUEST_REJECTED, "Investigate classification produced no signals"));
    }

    double dominance = contributions.values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0D);
    List<ClassificationType> contenders = contributions.entrySet().stream()
        .filter(entry -> dominance - entry.getValue() <= configuration.dominanceConfiguration().dominanceEpsilon())
        .map(Map.Entry::getKey)
        .toList();
    boolean boundary = contenders.stream()
        .anyMatch(contender -> contributions.getOrDefault(contender, 0D) >= configuration.dominanceConfiguration()
            .minimumConfidence());

    if (boundary
        && !contenders.contains(ClassificationType.LANDING)
        && !contenders.contains(ClassificationType.NOT_APPLICABLE)) {
      AnnotateRequest request = new AnnotateRequest(
          context.document().requestHeader(),
          context.document().target(),
          classification);
      Set<com.github.scholarfind.models.shared.ReasonCode> reasonCodes = state.reusedRecentClassification()
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
