package com.github.jelatinone.infra.store;

import com.github.jelatinone.infra.aws.store.DynamoStore;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ExecutionEventStore extends DynamoStore<TargetIdentity, ExecutionEvent> {
	public ExecutionEventStore(DynamoDbClient client) {
		super(
				client,
				new JacksonDynamoSerializer<>(
						ExecutionEvent.class,
						identity -> identity.identifier().toString()));
	}
}
