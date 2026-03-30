package com.github.scholarfind.infra.repository;

import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.ingest.TargetRecord;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class TargetRecordStore extends DynamoStore<TargetRecord, String> {
  public TargetRecordStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(TargetRecord.class, key -> key, TargetRecord::canonicalUrl));
  }
}
