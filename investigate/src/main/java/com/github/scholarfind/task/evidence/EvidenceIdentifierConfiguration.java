package com.github.scholarfind.task.evidence;

import java.util.List;

import com.github.scholarfind.task.signal.SignalIdentifier;

public record EvidenceIdentifierConfiguration(
		List<EvidenceRule> rules) {
	public List<EvidenceRule> rulesFor(SignalIdentifier identifier) {
		return rules.stream()
				.filter(rule -> rule.signal() == identifier)
				.toList();
	}
}
