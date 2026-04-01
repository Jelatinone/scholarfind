package com.github.jelatinone.infra.repository;

import java.util.UUID;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.serial.JacksonDynamoSerializer;
import com.github.jelatinone.models.annotate.AnnotateDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class AnnotateDocumentStore extends DynamoStore<AnnotateDocument, UUID> {
  public AnnotateDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(AnnotateDocument.class, UUID::toString,
            value -> value.target().targetId().toString()));
  }
}
