package com.github.jelatinone.model.annotate;

import java.util.Set;

import com.github.jelatinone.model.classification.Classification;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.struct.Identity.EdgeIdentity;
import com.github.jelatinone.model.struct.Identity.EntityIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record AnnotateDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,

    TargetIdentity targetId,
    ReviewIdentity reviewId,

    Classification.Annotate classification,

    Set<EdgeIdentity> producedEdges,
    Set<EntityIdentity> producedNodes

) implements Document<AnnotateDocument> {

  @Override
  public AnnotateDocument withDocumentHeader(DocumentHeader header) {
    return new AnnotateDocument(header, requestHeader(), header.targetId(), header.reviewId(), classification(),
        producedEdges(), producedNodes());
  }

  @Override
  public AnnotateDocument withRequestHeader(RequestHeader header) {
    return new AnnotateDocument(documentHeader(), header, header.targetId(), header.reviewId(), classification(),
        producedEdges(), producedNodes());
  }

}
