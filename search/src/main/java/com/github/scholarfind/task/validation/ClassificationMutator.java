package com.github.scholarfind.task.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.scholarfind.models.search.ClassificationType;
import com.github.scholarfind.models.search.SearchDocument;
import com.github.scholarfind.task.SearchContext;
import com.github.scholarfind.task.evidence.EvidenceRule;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalExtractionResult;
import com.github.scholarfind.task.signal.SignalExtractor;
import com.github.scholarfind.task.signal.SignalValue;
import com.github.scholarfind.utility.Builder;
import com.github.scholarfind.utility.Mutable;
import com.github.scholarfind.validation.Capability;
import com.github.scholarfind.validation.Mutator;

public class ClassificationMutator implements Mutator<SearchDocument, SearchContext> {

  @Override
  public Set<Capability> capabilities() {
    return Set.of(Capability.CLASSIFY);
  }

  @Override
  public void mutate(Builder<SearchDocument> builder, SearchContext context) {
    ClassificationConfiguration configuration = context.classificationConfiguration();

    Map<ClassificationType, Double> contributions = new HashMap<>();
    Mutable<SignalCost> cost = new Mutable<SignalCost>(configuration.costConfiguration().initial());

    configuration.costConfiguration().costs().forEach((identifier, signalCost) -> {
      if (SignalCost.max(signalCost, cost.value) != cost.value) {
        return;
      }

      SignalExtractor extractor = configuration.extractorConfiguration().extractor(identifier);
      SignalExtractionResult result = extractor.extract(context.document().trace(), context.retrievedContext());

      final Optional<SignalValue> value;
      switch (result) {
        case SignalExtractionResult.Both(SignalCost resultCost, Optional<SignalValue> resultValue) -> {
          value = resultValue;
          cost.value = (SignalCost.max(resultCost, cost.value));
        }

        case SignalExtractionResult.Value(Optional<SignalValue> resultValue) -> {
          value = resultValue;
        }

        case SignalExtractionResult.Cost(SignalCost resultCost) -> {
          value = Optional.empty();
          cost.value = (SignalCost.max(resultCost, cost.value));
        }
      }
      List<EvidenceRule> rules = configuration.evidenceConfiguration().rulesFor(identifier);

      rules.forEach(evidence -> {
        Double weight = configuration.scoreConfiguration().policyFor(evidence.classification())
            .score(evidence.rule().apply(value));

        ClassificationType classification = evidence.classification();
        contributions.merge(
            classification,
            weight.isInfinite() || weight.isNaN() ? 0D : weight,
            Double::sum);
      });
    });

    // TODO: Add To Builder
  }
}
