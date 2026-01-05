package com.github.scholarfind.task.score;

public record DominancePolicy(
		double dominanceEpsilon,
		double minimumConfidence) {
}
