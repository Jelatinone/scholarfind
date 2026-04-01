package com.github.jelatinone.models.ingest;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.TargetReference;

public record IngestRequest(
    RequestHeader requestHeader,
    TargetReference target,
    IngestProvenance provenance,
    Integer priority) implements Request {

  public static final long SCHEMA_VERSION = 1L;

  public static IngestRequest canonical(
      RequestHeader requestHeader,
      URL url,
      UUID parentTargetId,
      int depth,
      Instant discoveredAt,
      IngestProvenance provenance,
      Integer priority) {
    return new IngestRequest(
        requestHeader,
        TargetReference.canonical(url, parentTargetId, depth, discoveredAt),
        provenance,
        priority);
  }
}
