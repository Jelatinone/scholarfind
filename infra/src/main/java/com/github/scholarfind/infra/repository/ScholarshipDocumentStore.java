package com.github.scholarfind.infra.repository;

import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.scholarship.ScholarshipDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ScholarshipDocumentStore extends DynamoStore<ScholarshipDocument, String> {
  public ScholarshipDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(
            ScholarshipDocument.class,
            key -> key,
            value -> String.format("%s:%d", value.scholarshipId(), value.schemaVersion())));
  }
}
