package com.github.scholarfind.models.document.annotate;

import com.github.scholarfind.models.document.PreferenceLevel;

public record Activity(
                String activityDescriptor,
                ActivityType type,
                PreferenceLevel preferenceLevel) {
}