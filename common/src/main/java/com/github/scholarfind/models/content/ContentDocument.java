package com.github.scholarfind.models.content;

import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.TargetReference;

public record ContentDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    Capture capture,
    String fingerprint) implements StageDocument<ContentDocument> {

  public static final long SCHEMA_VERSION = 1L;

  @Override
  public ContentDocument withDocumentHeader(DocumentHeader header) {
    return new ContentDocument(header, requestHeader, target, capture, fingerprint);
  }

  @Override
  public ContentDocument withRequestHeader(RequestHeader header) {
    return new ContentDocument(documentHeader, header, target, capture, fingerprint);
  }
}
