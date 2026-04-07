package com.github.jelatinone.task.score;

import java.util.Map;

import com.github.jelatinone.models.investigate.ClassificationKind;

public record ScoreRuleConfiguration(
    Map<ClassificationKind, ScoreRule> rules) {
  public ScoreRule policyFor(ClassificationKind classification) {
    return rules.get(classification);
  }
}
