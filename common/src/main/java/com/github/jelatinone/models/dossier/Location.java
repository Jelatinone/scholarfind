package com.github.jelatinone.models.dossier;

public record Location(
		String descriptor,
		LocationType type,
		PreferenceLevel magnitude) {
}
