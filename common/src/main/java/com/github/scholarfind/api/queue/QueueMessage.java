package com.github.scholarfind.api.queue;

import java.util.Map;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public interface QueueMessage {
	String body();

	Map<String, MessageAttributeValue> attributes();
}
