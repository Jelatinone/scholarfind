package com.github.jelatinone.infra.store;

import com.github.jelatinone.infra.aws.store.DynamoStore;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.model.audit.AttemptEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class AttemptEventStore extends DynamoStore<AttemptEvent, String> {
  public AttemptEventStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(
            AttemptEvent.class,
            key -> key,
            value -> String.format(
                "%s:%s:%s:%s",
                value.requestId(),
                value.reviewId(),
                value.targetId(),
                value.occurredAt())));
  }
}
