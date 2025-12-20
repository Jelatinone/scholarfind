package com.github.scholarfind.models.document.annotate;

import com.github.scholarfind.models.document.PreferenceLevel;

public record Supplemental(
        String supplementDescriptor,
        SupplementalType type,
        PreferenceLevel preferenceLevel) {
}
