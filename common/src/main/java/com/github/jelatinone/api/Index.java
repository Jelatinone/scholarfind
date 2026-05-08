package com.github.jelatinone.api;

public record Index<Prop extends Property>(
    String canonicalName,
    Class<Prop> propertyType) {
}
