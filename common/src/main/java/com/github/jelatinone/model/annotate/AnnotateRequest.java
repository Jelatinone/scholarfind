package com.github.jelatinone.model.annotate;

import java.net.URL;

import com.github.jelatinone.model.classification.Classification;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public sealed interface AnnotateRequest extends com.github.jelatinone.model.struct.Request<AnnotateRequest>
    permits AnnotateRequest.Normal, AnnotateRequest.Learn {

  @Override
  RequestHeader requestHeader();

  @Override
  TargetIdentity targetId();

  @Override
  ReviewIdentity reviewId();

  URL canonicalUrl();

  public record Normal(
      RequestHeader requestHeader,

      TargetIdentity targetId,
      ReviewIdentity reviewId,

      URL canonicalUrl,

      Classification.Investigate classification) implements AnnotateRequest {

    @Override
    public AnnotateRequest withRequestHeader(RequestHeader header) {
      return new Normal(header, header.targetId(), header.reviewId(), canonicalUrl(), classification());
    }
  }

  public record Learn(
      RequestHeader requestHeader,

      TargetIdentity targetId,
      ReviewIdentity reviewId,

      URL canonicalUrl) implements AnnotateRequest {

    @Override
    public AnnotateRequest withRequestHeader(RequestHeader header) {
      return new Learn(header, header.targetId(), header.reviewId(), canonicalUrl());
    }
  }
}
