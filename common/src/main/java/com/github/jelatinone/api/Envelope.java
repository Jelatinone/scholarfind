package com.github.jelatinone.api;

public record Envelope<T>(T document, Acknowledgement acknowledgement) {
}
