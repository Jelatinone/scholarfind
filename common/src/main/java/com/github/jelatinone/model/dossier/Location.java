package com.github.jelatinone.model.dossier;

public record Location(
		String descriptor,
		LocationType type,
		PreferenceLevel magnitude) {
}
