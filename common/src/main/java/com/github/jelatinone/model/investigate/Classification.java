package com.github.jelatinone.model.investigate;

import java.util.Map;
import java.util.Set;

public sealed interface Classification {

  record Missing() implements Classification {
  }

  record Collected(
      Map<Category, Double> categoryEstimates,

      Double contenderConfidence,
      Set<Category> contenderCategories) implements Classification {
  }

  record Processed(
      Map<Category, Double> categoryEstimates,

      Double contenderConfidence,
      Set<Category> contenderCategories,

      Category mostPragmaticCategory,
      boolean minimumConfienceExceeded) implements Classification {
  }
}
