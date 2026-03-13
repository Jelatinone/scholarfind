package com.github.scholarfind.models.ingest;

import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.TargetReference;

public record IngestRequest(
    RequestHeader requestHeader,
    TargetReference target,
    IngestOrigin origin,
    Integer priority) implements Request {

  public static final long schemaVersion = 1L;
}
