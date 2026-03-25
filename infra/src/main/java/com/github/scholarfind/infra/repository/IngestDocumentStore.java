package com.github.scholarfind.infra.repository;

import java.util.UUID;


import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.ingest.IngestDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class IngestDocumentStore extends DynamoStore<IngestDocument, UUID> {
  public IngestDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(IngestDocument.class, UUID::toString,
            value -> value.target().targetId().toString()));
  }
}
