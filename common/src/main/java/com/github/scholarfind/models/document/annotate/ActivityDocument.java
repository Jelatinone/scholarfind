package com.github.scholarfind.models.document.annotate;

import com.github.scholarfind.models.document.PreferenceLevel;

public record ActivityDocument(
    String activityName,
    ActivityType type,
    PreferenceLevel preferenceLevel) {
}