package com.github.jelatinone.model.investigate;

import java.util.UUID;

import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;

import lombok.NonNull;

public record InvestigateRequest(
    @NonNull RequestHeader requestHeader,

    TargetNode target,

    @NonNull UUID targetId,
    @NonNull UUID reviewId
) implements Request {

  public InvestigateRequest(RequestHeader requestHeader, TargetNode target) {
    this(
        requestHeader,
        target,
        target == null ? requestHeader.targetId() : target.targetId(),
        requestHeader.reviewId());
  }

  public InvestigateRequest(RequestHeader requestHeader, UUID targetId, UUID reviewId) {
    this(requestHeader, null, targetId, reviewId);
  }
}
