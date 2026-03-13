package com.github.scholarfind.meta.transitory;

import java.time.Duration;

import com.github.scholarfind.models.shared.Request;

public record Emission<R extends Request>(
    R request,
    Duration delay,
    Integer priority,
    String dedupeKey) {
}
