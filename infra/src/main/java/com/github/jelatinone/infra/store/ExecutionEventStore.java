package com.github.jelatinone.infra.store;

import java.util.UUID;

import com.github.jelatinone.infra.aws.store.DynamoStore;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.model.audit.ExecutionEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ExecutionEventStore extends DynamoStore<ExecutionEvent, UUID> {
	public ExecutionEventStore(DynamoDbClient client, String table) {
		super(
				client,
				table,
				new JacksonDynamoSerializer<>(
						ExecutionEvent.class,
						UUID::toString));
	}
}
