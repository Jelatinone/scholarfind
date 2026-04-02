package com.github.jelatinone.task.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.models.investigate.ClassificationKind;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.utility.Mutable;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;
import com.github.jelatinone.task.evidence.EvidenceRule;
import com.github.jelatinone.task.signal.SignalCost;
import com.github.jelatinone.task.signal.SignalExtractionResult;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalValue;

public final class ClassificationPolicy implements Policy<InvestigateContext, InvestigateState> {

  @Override
  public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
    if (state.classificationResolved()) {
      return new PolicyStep.Continue<>(state);
    }

    ClassificationConfiguration configuration = context.classificationConfiguration();
    Map<ClassificationKind, Double> contributions = new HashMap<>();
    Mutable<SignalCost> cost = new Mutable<>(configuration.costConfiguration().initial());

    configuration.costConfiguration().costs().forEach((identifier, signalCost) -> {
      if (SignalCost.max(signalCost, cost.value) != cost.value) {
        return;
      }

      SignalExtractor extractor = configuration.extractorConfiguration().extractor(identifier);
      SignalExtractionResult result = extractor.extract(context.document().trace(), context.retrievedContent());

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

    ClassificationStub classification = new ClassificationStub(contributions);
    double confidence = contributions.values().stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0D);
    return new PolicyStep.Continue<>(state.withClassification(classification, confidence, false));
  }
}
