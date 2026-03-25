package com.github.scholarfind.infra.repository;

import java.util.UUID;


import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.serial.JacksonDynamoSerializer;
import com.github.scholarfind.models.investigate.InvestigateDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class InvestigateDocumentStore extends DynamoStore<InvestigateDocument, UUID> {
  public InvestigateDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(InvestigateDocument.class, UUID::toString,
            value -> value.target().targetId().toString()));
  }
}
