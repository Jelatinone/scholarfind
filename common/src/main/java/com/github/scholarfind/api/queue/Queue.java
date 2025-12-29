package com.github.scholarfind.api.queue;

import java.util.Map;

import lombok.NonNull;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public interface Queue extends AutoCloseable {

	QueueResult poll(int messageCount);

	void send(@NonNull String message, @NonNull Map<String, MessageAttributeValue> attributes);
}
