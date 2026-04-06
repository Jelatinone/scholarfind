package com.github.jelatinone.acquisition;

public record InterpretedContent(
    byte[] rawSource,
    String normalizedSource,
    String normalizedPreview) {
}
