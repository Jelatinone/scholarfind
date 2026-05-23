package com.github.jelatinone.model.classification;

import java.util.Map;
import java.util.Set;

import com.github.jelatinone.acquisition.Acquisition.Rank;

public sealed interface Classification {

  record Investigate(
      Map<Category, Double> categoryEstimates,

      Double contenderConfidence,
      Set<Category> contenderCategories,

      Rank mostPragmaticRank,
      Category mostPragmaticCategory,

      boolean minimumConfienceExceeded) implements Classification {
  }

  record Annotate(
      Map<Category, Double> categoryEstimates,

      Double contenderConfidence,
      Set<Category> contenderCategories,

      Category mostPragmaticCategory,

      boolean minimumConfienceExceeded) implements Classification {
  }
}
