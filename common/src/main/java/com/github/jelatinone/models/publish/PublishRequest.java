package com.github.jelatinone.models.publish;

import com.github.jelatinone.models.scholarship.ScholarshipDocument;
import com.github.jelatinone.models.shared.FetchReference;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.TargetReference;

public record PublishRequest(
    RequestHeader requestHeader,
    TargetReference target,
    FetchReference snapshot,
    ScholarshipDocument scholarship) implements Request {

  public static final long SCHEMA_VERSION = 1L;
}
