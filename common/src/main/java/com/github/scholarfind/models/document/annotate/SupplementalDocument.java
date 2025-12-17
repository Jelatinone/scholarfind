package com.github.scholarfind.models.document.annotate;

import com.github.scholarfind.models.document.PreferenceLevel;

public record SupplementalDocument(
    String supplementName,
    SupplementalType type,
    PreferenceLevel preferenceLevel) {
}
