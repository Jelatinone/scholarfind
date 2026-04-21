package com.github.jelatinone.api;

public record Envelope<Content>(Content content, Acknowledgement acknowledgement) {
}
