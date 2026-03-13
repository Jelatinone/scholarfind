package com.github.scholarfind.models.shared;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

public record TargetReference(
    UUID targetId,
    URL normalizedUrl,
    UUID parentTargetId,
    int depth,
    Instant discoveredAt) {
}
