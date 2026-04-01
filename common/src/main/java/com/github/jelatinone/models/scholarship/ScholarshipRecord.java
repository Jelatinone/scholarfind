package com.github.jelatinone.models.scholarship;

import java.time.Instant;
import java.util.UUID;

public record ScholarshipRecord(
        UUID scholarshipId,
        long currentVersion,
        ScholarshipState state,
        Instant createdAt,
        Instant updatedAt) {
}
