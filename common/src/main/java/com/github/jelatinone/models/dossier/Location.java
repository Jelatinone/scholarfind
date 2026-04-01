package com.github.jelatinone.models.dossier;

public record Location(
        LocationLevel level,
        String value,
        PreferenceLevel preference) {
}
