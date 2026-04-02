package com.github.jelatinone.models.annotate;

import java.util.Collection;

import com.github.jelatinone.models.shared.DocumentHeader;
import com.github.jelatinone.models.shared.FetchReference;
import com.github.jelatinone.models.shared.ReasonCode;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.models.shared.TraceReference;

public record AnnotateDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    TraceReference trace,
    FetchReference snapshot,
    AnnotateStub annotation,
    Double extractionConfidence,
    Collection<ReasonCode> qualityFlags,
    int discoveredTargetCount) implements StageDocument<AnnotateDocument> {

  public static final long SCHEMA_VERSION = 1L;

  @Override
  public AnnotateDocument withDocumentHeader(DocumentHeader header) {
    return new AnnotateDocument(
        header,
        requestHeader,
        target,
        trace,
        snapshot,
        annotation,
        extractionConfidence,
        qualityFlags,
        discoveredTargetCount);
  }

  @Override
  public AnnotateDocument withRequestHeader(RequestHeader header) {
    return new AnnotateDocument(
        documentHeader,
        header,
        target,
        trace,
        snapshot,
        annotation,
        extractionConfidence,
        qualityFlags,
        discoveredTargetCount);
  }
}
