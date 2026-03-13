package com.github.scholarfind.models.investigate;

import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.TargetReference;

public record InvestigateRequest(
    RequestHeader requestHeader,
    TargetReference target) implements Request {

  public static final long schemaVersion = 1L;
}
