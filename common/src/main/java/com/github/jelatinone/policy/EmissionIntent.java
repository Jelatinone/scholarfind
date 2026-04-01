package com.github.jelatinone.policy;

import java.time.Duration;

import com.github.jelatinone.models.shared.Request;

public record EmissionIntent<R extends Request>(
        R request,
        Duration delay,
        Integer priority,
        String dedupeKey) {
}
