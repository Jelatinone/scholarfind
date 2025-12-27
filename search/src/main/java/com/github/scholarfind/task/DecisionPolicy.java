package com.github.scholarfind.task;

public record DecisionPolicy(
		double dominanceEpsilon,
		double minimumConfidence) {
}
