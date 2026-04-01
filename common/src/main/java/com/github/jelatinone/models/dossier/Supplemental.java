package com.github.jelatinone.models.dossier;

public record Supplemental(
        String supplementDescriptor,
        SupplementalType type,
        PreferenceLevel preferenceLevel) {
}
