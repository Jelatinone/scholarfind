package com.github.jelatinone.model.transit;

import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public record RequestIntent<R extends Request>(
    @NonNull UUID intentId,
    @NonNull Letter<R> letter,
    @NonNull Instant emittedAt) {
}
