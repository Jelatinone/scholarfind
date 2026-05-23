package com.github.jelatinone.model.annotate;

import java.net.URL;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record AnnotateRequest(
    RequestHeader requestHeader,

    TargetIdentity targetId,
    ReviewIdentity reviewId,

    URL canonicalUrl,

    AnnotateIntent intent) implements Request<AnnotateRequest> {

  @Override
  public AnnotateRequest withRequestHeader(RequestHeader header) {
    return new AnnotateRequest(header, header.targetId(), header.reviewId(), canonicalUrl(), intent());
  }
}