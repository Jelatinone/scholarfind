package com.github.scholarfind.api.queue;

public record ReceivedMessage<T>(
    T message,
    Acknowledgement acknowledgement) {
}
