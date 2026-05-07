package com.github.jelatinone.task.score;

import java.util.Map;

import com.github.jelatinone.model.investigate.Category;

public record ScoreRuleConfiguration(
    Map<Category, ScoreRule> rules) {
  public ScoreRule policyFor(Category classification) {
    return rules.get(classification);
  }
}
