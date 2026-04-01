package com.github.jelatinone.api.queue;

import com.github.jelatinone.api.Acknowledgement;

public record ReceivedMessage<T>(
        T message,
        Acknowledgement acknowledgement) {
}
