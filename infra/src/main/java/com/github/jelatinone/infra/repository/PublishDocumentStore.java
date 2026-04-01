package com.github.jelatinone.infra.repository;

import java.util.UUID;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.serial.JacksonDynamoSerializer;
import com.github.jelatinone.models.publish.PublishDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class PublishDocumentStore extends DynamoStore<PublishDocument, UUID> {
  public PublishDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(PublishDocument.class, UUID::toString,
            value -> value.target().targetId().toString()));
  }
}
