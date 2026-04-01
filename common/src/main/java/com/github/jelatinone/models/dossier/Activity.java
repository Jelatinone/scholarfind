package com.github.jelatinone.models.dossier;

public record Activity(
        String activityDescriptor,
        ActivityType type,
        PreferenceLevel preferenceLevel) {
}
