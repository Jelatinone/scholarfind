package com.github.scholarfind.api.queue;

import java.util.List;

public record QueueResult(
		List<ReceivedMessage> messages,
		QueueState state) {

}
