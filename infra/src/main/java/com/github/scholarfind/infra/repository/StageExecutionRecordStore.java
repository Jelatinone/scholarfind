package com.github.scholarfind.infra.repository;



import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.audit.StageExecution;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class StageExecutionRecordStore extends DynamoStore<StageExecution, String> {
  public StageExecutionRecordStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(
            StageExecution.class,
            key -> key,
            value -> StageExecution.key(value.targetId(), value.stage())));
  }
}
