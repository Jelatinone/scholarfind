package com.github.jelatinone.model.dossier;

public record Activity(
		String descriptor,
		ActivityType type,
		PreferenceLevel magnitude) {
}
