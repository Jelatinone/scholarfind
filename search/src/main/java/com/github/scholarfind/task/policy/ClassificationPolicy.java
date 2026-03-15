package com.github.scholarfind.task.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.ClassificationType;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.SearchContext;
import com.github.scholarfind.task.SearchState;
import com.github.scholarfind.task.evidence.EvidenceRule;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalExtractionResult;
import com.github.scholarfind.task.signal.SignalExtractor;
import com.github.scholarfind.task.signal.SignalValue;
import com.github.scholarfind.utility.Mutable;

public final class ClassificationPolicy implements Policy<SearchContext, SearchState> {

  @Override
  public PolicyStep<SearchState> apply(SearchContext context, SearchState state) {
    if (state.classificationResolved()) {
      return new PolicyStep.Continue<>(state);
    }

    ClassificationConfiguration configuration = context.classificationConfiguration();
    Map<ClassificationType, Double> contributions = new HashMap<>();
    Mutable<SignalCost> cost = new Mutable<>(configuration.costConfiguration().initial());

    configuration.costConfiguration().costs().forEach((identifier, signalCost) -> {
      if (SignalCost.max(signalCost, cost.value) != cost.value) {
        return;
      }

      SignalExtractor extractor = configuration.extractorConfiguration().extractor(identifier);
      SignalExtractionResult result = extractor.extract(context.document().trace(), context.retrievedContext());

      Optional<SignalValue> value;
      switch (result) {
        case SignalExtractionResult.Both(SignalCost resultCost, Optional<SignalValue> resultValue) -> {
          value = resultValue;
          cost.value = SignalCost.max(resultCost, cost.value);
        }
        case SignalExtractionResult.Value(Optional<SignalValue> resultValue) -> {
          value = resultValue;
        }
        case SignalExtractionResult.Cost(SignalCost resultCost) -> {
          value = Optional.empty();
          cost.value = SignalCost.max(resultCost, cost.value);
        }
      }

      List<EvidenceRule> rules = configuration.evidenceConfiguration().rulesFor(identifier);
      rules.forEach(evidence -> {
        Double weight = configuration.scoreConfiguration()
            .policyFor(evidence.classification())
            .score(evidence.rule().apply(value));
        double sanitized = weight.isInfinite() || weight.isNaN() ? 0D : weight;
        contributions.merge(evidence.classification(), sanitized, Double::sum);
      });
    });

    Classification classification = new Classification(contributions);
    double confidence = contributions.values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0D);
    return new PolicyStep.Continue<>(state.withClassification(classification, confidence, false));
  }
}
