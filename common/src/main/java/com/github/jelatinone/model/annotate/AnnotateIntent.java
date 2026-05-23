package com.github.jelatinone.model.annotate;

import com.github.jelatinone.model.classification.Classification;

public sealed interface AnnotateIntent {

  record Classified(
      Classification.Investigate classification) implements AnnotateIntent {
  }

  record Forwarded() implements AnnotateIntent {
  }
}
