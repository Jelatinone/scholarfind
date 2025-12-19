package com.github.scholarfind.models.document.annotate;

import com.github.scholarfind.models.document.PreferenceLevel;

public record ActivityDocument(
        String activityDescriptor,
        ActivityType type,
        PreferenceLevel preferenceLevel) {
}