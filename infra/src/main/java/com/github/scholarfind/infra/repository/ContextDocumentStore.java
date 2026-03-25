package com.github.scholarfind.infra.repository;

import java.util.UUID;


import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.shared.ContextDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ContextDocumentStore extends DynamoStore<ContextDocument, UUID> {
  public ContextDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(ContextDocument.class, UUID::toString,
            value -> value.target().targetId().toString()));
  }
}
