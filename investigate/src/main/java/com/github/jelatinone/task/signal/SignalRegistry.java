package com.github.jelatinone.task.signal;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SignalRegistry {

	private final Map<SignalIdentifier, SignalExtractor> extractors;

	public SignalRegistry(Set<SignalExtractor> extractors) {
		this.extractors = extractors.stream()
				.collect(Collectors.toMap(SignalExtractor::identifier, entry -> entry));
	}

	public SignalExtractor extractor(SignalIdentifier identifier) {
		return extractors.get(identifier);
	}
}
