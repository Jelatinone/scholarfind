package com.github.jelatinone.meta.transitory;

import java.time.Duration;

import com.github.jelatinone.models.shared.Request;

public record Emission<R extends Request>(
        R request,
        Duration delay,
        Integer priority,
        String dedupeKey) {
}
