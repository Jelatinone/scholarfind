package com.github.jelatinone.api.queue;

import com.github.jelatinone.api.Acknowledgement;

public record QueueEnvelope<Content>(Content content, Acknowledgement acknowledgement) {
}
