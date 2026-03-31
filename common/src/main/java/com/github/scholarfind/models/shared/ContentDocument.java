package com.github.scholarfind.models.shared;

public record ContentDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    String rawContent,
    String fingerprint) implements StageDocument<ContentDocument> {

  public static final long SCHEMA_VERSION = 1L;

  @Override
  public ContentDocument withDocumentHeader(DocumentHeader header) {
    return new ContentDocument(header, requestHeader, target, rawContent, fingerprint);
  }

  @Override
  public ContentDocument withRequestHeader(RequestHeader header) {
    return new ContentDocument(documentHeader, header, target, rawContent, fingerprint);
  }
}
