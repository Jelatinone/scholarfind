package com.github.scholarfind.task.score;

public record ScoreRule(
		double scale,
		double bias,
		double min,
		double max) {

	public double apply(double raw) {
		double v = raw * scale + bias;
		return Math.max(min, Math.min(max, v));
	}
}