package com.github.scholarfind.models.dossier;

public record Supplemental(
    String supplementDescriptor,
    SupplementalType type,
    PreferenceLevel preferenceLevel) {
}
