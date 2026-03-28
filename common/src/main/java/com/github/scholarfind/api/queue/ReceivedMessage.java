package com.github.scholarfind.api.queue;

import com.github.scholarfind.api.Acknowledgement;

public record ReceivedMessage<T>(
    T message,
    Acknowledgement acknowledgement) {
}
