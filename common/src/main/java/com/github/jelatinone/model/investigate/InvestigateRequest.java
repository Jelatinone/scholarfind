package com.github.jelatinone.model.investigate;

import java.net.URL;

import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record InvestigateRequest(
    RequestHeader requestHeader,

    TargetIdentity targetId,
    ReviewIdentity reviewId,

    URL canonicalUrl,

    KernelContext context

) implements Request<InvestigateRequest> {

  @Override
  public InvestigateRequest withRequestHeader(RequestHeader nextHeader) {
    return new InvestigateRequest(nextHeader, targetId(), reviewId(), canonicalUrl(), context());
  }

  public record KernelContext(
  // TODO: What context can we include without being expensive per-target?

  ) {

  }
}
