package com.github.scholarfind.task.signal;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SignalExtractorRegistry {

	private final Map<SignalIdentifier, SignalExtractor> _extractors;

	public SignalExtractorRegistry(Set<SignalExtractor> extractors) {
		this._extractors = extractors.stream()
				.collect(Collectors.toMap(SignalExtractor::identifier, entry -> entry));
	}

	public SignalExtractor extractor(SignalIdentifier identifier) {
		return _extractors.get(identifier);
	}
}
