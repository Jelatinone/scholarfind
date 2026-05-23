package com.github.jelatinone.infra.store;

import com.github.jelatinone.infra.aws.store.DynamoStore;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class AttemptEventStore extends DynamoStore<TargetIdentity, AttemptEvent> {
	public AttemptEventStore(DynamoDbClient client) {
		super(
				client,
				new JacksonDynamoSerializer<>(
						AttemptEvent.class,
						identity -> identity.identifier().toString()));
	}
}
