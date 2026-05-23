package com.github.jelatinone.model.struct;

import com.github.jelatinone.model.graph.GraphNode;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

/**
 * 
 * <h1>Request</h1>
 * 
 * Represents the intermediate request made to perform a stage
 * operation on a given {@link GraphNode node}.
 * 
 * @author Cody Washington
 */
public interface Request<Self> {

  @NonNull
  RequestHeader requestHeader();

  @NonNull
  TargetIdentity targetId();

  @NonNull
  ReviewIdentity reviewId();

  Self withRequestHeader(RequestHeader header);
}
