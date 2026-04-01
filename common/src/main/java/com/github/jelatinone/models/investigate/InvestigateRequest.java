package com.github.jelatinone.models.investigate;

import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.TargetReference;

public record InvestigateRequest(
    RequestHeader requestHeader,
    TargetReference target) implements Request {

  public static final long SCHEMA_VERSION = 1L;
}
