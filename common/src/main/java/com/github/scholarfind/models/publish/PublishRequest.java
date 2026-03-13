package com.github.scholarfind.models.publish;

import com.github.scholarfind.models.scholarship.ScholarshipDocument;
import com.github.scholarfind.models.shared.FetchReference;
import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.TargetReference;

public record PublishRequest(
    RequestHeader requestHeader,
    TargetReference target,
    FetchReference snapshot,
    ScholarshipDocument scholarship) implements Request {

  public static final long schemaVersion = 1L;
}
