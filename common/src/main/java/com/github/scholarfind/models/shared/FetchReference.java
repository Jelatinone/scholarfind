package com.github.scholarfind.models.shared;

import java.time.Instant;

public record FetchReference(
    String snapshotId,
    String contentHash,
    Instant fetchedAt) {
}
