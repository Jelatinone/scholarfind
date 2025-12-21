package com.github.scholarfind.models.annotate;

import com.github.scholarfind.models.PreferenceLevel;

public record Activity(
    String activityDescriptor,
    ActivityType type,
    PreferenceLevel preferenceLevel) {
}