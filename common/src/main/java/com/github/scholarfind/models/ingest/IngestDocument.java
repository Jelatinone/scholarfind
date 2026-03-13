package com.github.scholarfind.models.ingest;

import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.TargetReference;

public record IngestDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    IngestOrigin origin,
    boolean admitted,
    String schedulingDecision,
    Integer depthBudget) implements StageDocument<IngestDocument> {

  public static final long schemaVersion = 1L;

  @Override
  public IngestDocument withDocumentHeader(DocumentHeader header) {
    return new IngestDocument(header, requestHeader, target, origin, admitted, schedulingDecision, depthBudget);
  }

  @Override
  public IngestDocument withRequestHeader(RequestHeader header) {
    return new IngestDocument(documentHeader, header, target, origin, admitted, schedulingDecision, depthBudget);
  }
}
