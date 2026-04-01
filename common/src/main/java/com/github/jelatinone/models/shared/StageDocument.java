package com.github.jelatinone.models.shared;

public interface StageDocument<Self extends StageDocument<Self>> {
  DocumentHeader documentHeader();

  RequestHeader requestHeader();

  TargetReference target();

  Self withDocumentHeader(DocumentHeader header);

  Self withRequestHeader(RequestHeader header);
}
