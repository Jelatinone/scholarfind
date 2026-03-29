package com.github.scholarfind.models.ingest;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.TargetReference;

public record IngestRequest(
    RequestHeader requestHeader,
    TargetReference target,
    IngestOrigin origin,
    Integer priority) implements Request {

  public static final long schemaVersion = 1L;

  public static IngestRequest canonical(
      RequestHeader requestHeader,
      URL url,
      UUID parentTargetId,
      int depth,
      Instant discoveredAt,
      IngestOrigin origin,
      Integer priority) {
    return new IngestRequest(
        requestHeader,
        TargetReference.canonical(url, parentTargetId, depth, discoveredAt),
        origin,
        priority);
  }
}
