package com.github.scholarfind.infra.repository;

import java.util.UUID;
import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.annotate.AnnotateDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class AnnotateDocumentStore extends DynamoStore<AnnotateDocument, UUID> {
  public AnnotateDocumentStore(DynamoDbClient client, String table, BiConsumer<String, Level> logger) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(AnnotateDocument.class, UUID::toString,
            value -> value.target().targetId().toString()),
        logger);
  }
}
