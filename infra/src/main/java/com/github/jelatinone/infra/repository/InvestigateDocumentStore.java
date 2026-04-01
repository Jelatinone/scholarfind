package com.github.jelatinone.infra.repository;

import java.util.UUID;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.serial.JacksonDynamoSerializer;
import com.github.jelatinone.models.investigate.InvestigateDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class InvestigateDocumentStore extends DynamoStore<InvestigateDocument, UUID> {
  public InvestigateDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(InvestigateDocument.class, UUID::toString,
            InvestigateDocumentStore::documentKey));
  }

  private static String documentKey(InvestigateDocument document) {
    if (document.target() != null && document.target().targetId() != null) {
      return document.target().targetId().toString();
    }
    if (document.documentHeader() != null && document.documentHeader().targetId() != null) {
      return document.documentHeader().targetId().toString();
    }
    return document.documentHeader().documentId().toString();
  }
}
