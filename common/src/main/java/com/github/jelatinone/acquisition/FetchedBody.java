package com.github.jelatinone.acquisition;

public record FetchedBody(
    FetchedMetadata metadata,
    byte[] body) {
}
