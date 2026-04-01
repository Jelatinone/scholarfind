package com.github.jelatinone.models.publish;

import java.util.UUID;

import com.github.jelatinone.models.shared.DocumentHeader;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.TargetReference;

public record PublishDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    UUID scholarshipId,
    long version,
    String publishDecision,
    String canonicalMatchKey,
    UUID sourceDocumentId) implements StageDocument<PublishDocument> {

  public static final long SCHEMA_VERSION = 1L;

  @Override
  public PublishDocument withDocumentHeader(DocumentHeader header) {
    return new PublishDocument(header, requestHeader, target, scholarshipId, version, publishDecision,
        canonicalMatchKey, sourceDocumentId);
  }

  @Override
  public PublishDocument withRequestHeader(RequestHeader header) {
    return new PublishDocument(documentHeader, header, target, scholarshipId, version, publishDecision,
        canonicalMatchKey, sourceDocumentId);
  }
}
