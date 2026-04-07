package com.github.jelatinone.infra.repository;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.jackson.JacksonDynamoSerializer;
import com.github.jelatinone.models.ingest.TargetRecord;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class TargetRecordStore extends DynamoStore<TargetRecord, String> {
  public TargetRecordStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(TargetRecord.class, key -> key, TargetRecord::canonicalUrl));
  }
}
