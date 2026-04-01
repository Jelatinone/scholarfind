package com.github.jelatinone.infra.repository;

import java.util.UUID;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.serial.JacksonDynamoSerializer;
import com.github.jelatinone.models.scholarship.ScholarshipRecord;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ScholarshipRecordStore extends DynamoStore<ScholarshipRecord, UUID> {
  public ScholarshipRecordStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(ScholarshipRecord.class, UUID::toString,
            value -> value.scholarshipId().toString()));
  }
}
