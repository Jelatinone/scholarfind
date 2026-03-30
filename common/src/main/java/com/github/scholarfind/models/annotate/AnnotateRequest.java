package com.github.scholarfind.models.annotate;

import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.TargetReference;

public record AnnotateRequest(
    RequestHeader requestHeader,
    TargetReference target,
    Classification classification) implements Request {

  public static final long SCHEMA_VERSION = 1L;
}
