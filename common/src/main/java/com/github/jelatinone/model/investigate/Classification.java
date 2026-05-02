package com.github.jelatinone.model.investigate;

import java.util.Map;
import java.util.Set;

public record Classification(
		Map<Category, Double> categoryEstimates,

		Double contenderConfidence,
		Set<Category> contenderCategories

) {

}
