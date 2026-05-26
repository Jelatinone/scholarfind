package com.github.jelatinone.model.classification;

import java.util.Map;
import java.util.Set;

import com.github.jelatinone.acquisition.Acquisition.Rank;
import com.github.jelatinone.utility.Normal;

public sealed interface Classification {

  record Investigate(
      Map<Category, Normal<Double>> categoryEstimates,

      Normal<Double> contenderConfidence,
      Set<Category> contenderCategories,

      Rank mostAcquiredRank,
      Category mostPragmaticCategory,

      boolean minimumConfienceExceeded) implements Classification {
  }

  record Annotate(
      Map<Category, Normal<Double>> categoryEstimates,

      Normal<Double> contenderConfidence,
      Set<Category> contenderCategories,

      Category mostPragmaticCategory,

      boolean minimumConfienceExceeded) implements Classification {
  }
}
