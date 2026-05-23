package com.github.jelatinone.model.struct;

import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

public interface Document<Self> {

  @NonNull
  DocumentHeader documentHeader();

  @NonNull
  RequestHeader requestHeader();

  @NonNull
  TargetIdentity targetId();

  @NonNull
  ReviewIdentity reviewId();

  Self withDocumentHeader(DocumentHeader header);

  Self withRequestHeader(RequestHeader header);
}
