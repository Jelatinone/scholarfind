package com.github.scholarfind.models.annotate;

import com.github.scholarfind.models.PreferenceLevel;

public record Supplemental(
                String supplementDescriptor,
                SupplementalType type,
                PreferenceLevel preferenceLevel) {
}
