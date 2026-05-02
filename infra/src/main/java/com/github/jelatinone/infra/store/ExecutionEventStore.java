package com.github.jelatinone.infra.store;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.serial.jackson.JacksonDynamoSerializer;
import com.github.jelatinone.model.audit.ExecutionEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ExecutionEventStore extends DynamoStore<ExecutionEvent, String> {
	public ExecutionEventStore(DynamoDbClient client, String table) {
		super(
				client,
				table,
				new JacksonDynamoSerializer<>(
						ExecutionEvent.class,
						key -> key,
						value -> String.format(
								"%s:%s:%s",
								value.targetId(),
								value.executionRef(),
								value.occurredAt())));
	}
}
