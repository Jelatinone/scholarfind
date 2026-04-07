package com.github.jelatinone.infra.repository;

import java.util.UUID;

import com.github.jelatinone.infra.aws.DynamoStore;
import com.github.jelatinone.infra.aws.jackson.JacksonDynamoSerializer;
import com.github.jelatinone.models.ingest.IngestDocument;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class IngestDocumentStore extends DynamoStore<IngestDocument, UUID> {
  public IngestDocumentStore(DynamoDbClient client, String table) {
    super(
        client,
        table,
        new JacksonDynamoSerializer<>(IngestDocument.class, UUID::toString,
            IngestDocumentStore::documentKey));
  }

  private static UUID documentKey(IngestDocument document) {
    if (document.target() != null && document.target().targetId() != null) {
      return document.target().targetId();
    }
    if (document.documentHeader() != null && document.documentHeader().targetId() != null) {
      return document.documentHeader().targetId();
    }
    return document.documentHeader().documentId();
  }
}
