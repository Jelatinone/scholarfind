package com.github.scholarfind.models.shared;

public record ContextDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    String rawContext,
    String fingerprint) implements StageDocument<ContextDocument> {

  public static final long SCHEMA_VERSION = 1L;

  @Override
  public ContextDocument withDocumentHeader(DocumentHeader header) {
    return new ContextDocument(header, requestHeader, target, rawContext, fingerprint);
  }

  @Override
  public ContextDocument withRequestHeader(RequestHeader header) {
    return new ContextDocument(documentHeader, header, target, rawContext, fingerprint);
  }
}
