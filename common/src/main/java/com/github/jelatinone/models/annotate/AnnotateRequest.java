package com.github.jelatinone.models.annotate;

import com.github.jelatinone.models.investigate.Classification;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.TargetReference;

public record AnnotateRequest(
    RequestHeader requestHeader,
    TargetReference target,
    Classification classification) implements Request {

  public static final long SCHEMA_VERSION = 1L;
}
