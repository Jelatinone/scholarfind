package com.github.jelatinone.model.struct;

import java.util.UUID;

import lombok.NonNull;

public interface Document<Self> {

  @NonNull
  DocumentHeader documentHeader();

  @NonNull
  RequestHeader requestHeader();

  @NonNull
  UUID targetId();

  @NonNull
  UUID reviewId();

  Self withDocumentHeader(DocumentHeader header);

  Self withRequestHeader(RequestHeader header);
}
