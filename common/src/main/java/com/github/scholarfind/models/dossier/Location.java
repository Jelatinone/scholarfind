package com.github.scholarfind.models.dossier;

public record Location(
    LocationLevel level,
    String value,
    PreferenceLevel preference) {
}
