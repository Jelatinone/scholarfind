package com.github.scholarfind.policy;

import java.time.Duration;

import com.github.scholarfind.models.shared.Request;

public record EmissionIntent<R extends Request>(
    R request,
    Duration delay,
    Integer priority,
    String dedupeKey) {
}
