package com.github.jelatinone.task.score;

public record ScoreRule(
		double scale,
		double bias,
		double min,
		double max) {

	public double score(double raw) {
		double value = raw * scale + bias;
		return Math.max(min, Math.min(max, value));
	}
}