package com.github.jelatinone.infra.store;

import java.util.UUID;

import com.github.jelatinone.infra.aws.store.DynamoStore;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.model.audit.AttemptEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class AttemptEventStore extends DynamoStore<UUID, AttemptEvent> {
	public AttemptEventStore(DynamoDbClient client) {
		super(
				client,
				new JacksonDynamoSerializer<>(
						AttemptEvent.class,
						UUID::toString));
	}
}
