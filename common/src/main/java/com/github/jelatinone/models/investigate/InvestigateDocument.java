package com.github.jelatinone.models.investigate;

import java.time.Instant;

import com.github.jelatinone.models.shared.DocumentHeader;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.models.shared.TraceReference;

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

  public static final long SCHEMA_VERSION = 1L;

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
