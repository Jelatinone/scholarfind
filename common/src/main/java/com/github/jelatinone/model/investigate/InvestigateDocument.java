package com.github.jelatinone.model.investigate;

import java.util.Set;

import com.github.jelatinone.model.classification.Classification;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.struct.Identity.EdgeIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record InvestigateDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,

    TargetIdentity targetId,
    ReviewIdentity reviewId,

    Classification.Investigate classification,
    Set<EdgeIdentity> producedEdges

) implements Document<InvestigateDocument> {

  @Override
  public InvestigateDocument withDocumentHeader(DocumentHeader header) {
    return new InvestigateDocument(header, requestHeader(), header.targetId(), header.reviewId(), classification(),
        producedEdges());
  }

  @Override
  public InvestigateDocument withRequestHeader(RequestHeader header) {
    return new InvestigateDocument(documentHeader(), header, header.targetId(), header.reviewId(), classification(),
        producedEdges());
  }

}
