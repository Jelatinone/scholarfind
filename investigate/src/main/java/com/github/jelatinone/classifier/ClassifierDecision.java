package com.github.jelatinone.classifier;

import com.github.jelatinone.model.classification.Classification;

public sealed interface ClassifierDecision {

  record Continue() implements ClassifierDecision {
  }

  record Decide(Classification classification) implements ClassifierDecision {
  }
}
