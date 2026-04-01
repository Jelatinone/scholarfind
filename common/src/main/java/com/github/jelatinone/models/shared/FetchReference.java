package com.github.jelatinone.models.shared;

import java.time.Instant;

public record FetchReference(
        String snapshotId,
        String contentHash,
        Instant fetchedAt) {
}
