package com.github.jelatinone.model.dossier;

public record Supplemental(
		String descriptor,
		SupplementalType type,
		PreferenceLevel magnitude) {
}
