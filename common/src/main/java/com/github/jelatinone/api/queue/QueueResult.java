package com.github.jelatinone.api.queue;

import java.util.List;

public record QueueResult<T>(
        List<ReceivedMessage<T>> messages,
        QueueState state) {

}
