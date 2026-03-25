package com.github.scholarfind.infra.repository;



import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.audit.AttemptEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class AttemptEventStore extends DynamoStore<AttemptEvent, String> {
  public AttemptEventStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(
            AttemptEvent.class,
            key -> key,
            value -> String.format("%s:%s:%d", value.requestId(), value.stage(), value.attempt())));
  }
}
