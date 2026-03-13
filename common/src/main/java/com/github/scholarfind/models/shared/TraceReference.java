package com.github.scholarfind.models.shared;

import java.net.URL;

public record TraceReference(
    URL normalizedUrl,
    URL normalizedParentUrl,
    String reviewer,
    Integer depth) {
}
