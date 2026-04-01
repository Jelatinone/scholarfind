package com.github.scholarfind.infra.repository;

import java.util.UUID;

import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.content.ContentDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ContentDocumentStore extends DynamoStore<ContentDocument, UUID> {
  public ContentDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(ContentDocument.class, UUID::toString,
            value -> value.target().targetId().toString()));
  }
}
