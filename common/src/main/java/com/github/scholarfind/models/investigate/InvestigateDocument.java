package com.github.scholarfind.models.investigate;

import java.time.Instant;

import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.TargetReference;
import com.github.scholarfind.models.shared.TraceReference;

@lombok.Builder
public record InvestigateDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    TraceReference trace,
    Instant reviewedAt,
    Classification classification,
    Double confidence,
    int discoveredTargetCount) implements StageDocument<InvestigateDocument> {

  public static final long schemaVersion = 1L;

  @Override
  public InvestigateDocument withDocumentHeader(DocumentHeader header) {
    return new InvestigateDocument(header, requestHeader, target, trace, reviewedAt, classification, confidence,
        discoveredTargetCount);
  }

  @Override
  public InvestigateDocument withRequestHeader(RequestHeader header) {
    return new InvestigateDocument(documentHeader, header, target, trace, reviewedAt, classification, confidence,
        discoveredTargetCount);
  }

  public InvestigateDocument withClassification(Classification nextClassification) {
    return new InvestigateDocument(documentHeader, requestHeader, target, trace, reviewedAt, nextClassification,
        confidence, discoveredTargetCount);
  }
}
