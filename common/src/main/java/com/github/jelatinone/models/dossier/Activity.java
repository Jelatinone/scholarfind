package com.github.jelatinone.models.dossier;

public record Activity(
		String descriptor,
		ActivityType type,
		PreferenceLevel magnitude) {
}
