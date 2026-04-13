package com.github.jelatinone.models.dossier;

public record Supplemental(
		String descriptor,
		SupplementalType type,
		PreferenceLevel magnitude) {
}
