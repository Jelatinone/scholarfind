package com.github.scholarfind.models.dossier;

public record Activity(
    String activityDescriptor,
    ActivityType type,
    PreferenceLevel preferenceLevel) {
}
