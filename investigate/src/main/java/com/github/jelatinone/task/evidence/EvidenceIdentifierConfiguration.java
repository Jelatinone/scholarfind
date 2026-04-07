package com.github.jelatinone.task.evidence;

import java.util.List;

import com.github.jelatinone.task.signal.SignalIdentity;

public record EvidenceIdentifierConfiguration(List<EvidenceRule> rules) {
  public List<EvidenceRule> rulesFor(SignalIdentity identifier) {
    return rules.stream()
        .filter(rule -> rule.identity() == identifier)
        .toList();
  }
}
