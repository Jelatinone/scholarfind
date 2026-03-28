package com.github.scholarfind.api;

public record Envelope<T>(T document, Acknowledgement acknowledgement) {
}
