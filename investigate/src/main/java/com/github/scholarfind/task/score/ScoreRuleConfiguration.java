package com.github.scholarfind.task.score;

import java.util.Map;

import com.github.scholarfind.models.investigate.ClassificationType;

public record ScoreRuleConfiguration(
		Map<ClassificationType, ScoreRule> rules) {
	public ScoreRule policyFor(ClassificationType classification) {
		return rules.get(classification);
	}
}
