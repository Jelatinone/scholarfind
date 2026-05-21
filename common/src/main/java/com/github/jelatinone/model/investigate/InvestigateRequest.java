package com.github.jelatinone.model.investigate;

import java.net.URL;
import java.util.UUID;

import com.github.jelatinone.acquisition.Acquisition.Rank;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;

public record InvestigateRequest(
    RequestHeader requestHeader,

    UUID targetId,
    UUID reviewId,

    URL canonicalUrl,

    KernelAllotment allotment,
    KernelContext context

) implements Request<InvestigateRequest> {

  @Override
  public InvestigateRequest withRequestHeader(RequestHeader nextHeader) {
    return new InvestigateRequest(nextHeader, targetId(), reviewId(), canonicalUrl(), allotment(), context());
  }

  public record KernelAllotment(

      int step,
      Rank rank) {
  }

  public record KernelContext(

      int upwardEdgeCount,
      int downwardEdgeCount,
      int reduceEdgeCount) {
  }
}
