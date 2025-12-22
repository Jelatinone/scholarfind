package com.github.scholarfind.task;

import com.github.scholarfind.models.search.ClassificationType;

public record WeightedCueValue(
		String string,
		ClassificationType classification,
		Double weight) {

}
