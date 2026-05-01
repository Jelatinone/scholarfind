package com.github.jelatinone.model.content;

import java.util.UUID;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;

import lombok.NonNull;

public record ContentDocument(
    @NonNull DocumentHeader documentHeader,
    @NonNull RequestHeader requestHeader,
    @NonNull Capture capture,

    String previewText,
    String contentHash) implements Document<ContentDocument> {

  public static final long SCHEMA_VERSION = 1L;

  @Override
  public UUID targetId() {
    return documentHeader.targetId();
  }

  @Override
  public UUID reviewId() {
    return documentHeader.reviewId();
  }

  @Override
  public ContentDocument withDocumentHeader(DocumentHeader header) {
    return new ContentDocument(header, requestHeader, capture, previewText, contentHash);
  }

  @Override
  public ContentDocument withRequestHeader(RequestHeader header) {
    return new ContentDocument(documentHeader, header, capture, previewText, contentHash);
  }
}
