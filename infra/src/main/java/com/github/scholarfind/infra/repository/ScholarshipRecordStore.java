package com.github.scholarfind.infra.repository;

import java.util.UUID;
import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.scholarship.ScholarshipRecord;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ScholarshipRecordStore extends DynamoStore<ScholarshipRecord, UUID> {
  public ScholarshipRecordStore(DynamoDbClient client, String table, BiConsumer<String, Level> logger) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(ScholarshipRecord.class, UUID::toString,
            value -> value.scholarshipId().toString()),
        logger);
  }
}
