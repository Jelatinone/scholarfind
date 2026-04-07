package com.github.jelatinone.infra.repository;

import java.util.UUID;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.jackson.JacksonDynamoSerializer;
import com.github.jelatinone.models.content.ContentDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class ContentDocumentStore extends DynamoStore<ContentDocument, UUID> {
  public ContentDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(ContentDocument.class, UUID::toString,
            value -> value.target().targetId()));
  }
}
