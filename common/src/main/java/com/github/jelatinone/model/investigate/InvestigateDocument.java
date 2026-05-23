package com.github.jelatinone.model.investigate;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record InvestigateDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,

    TargetIdentity targetId,
    ReviewIdentity reviewId,

    InvestigateResult result

) implements Document<InvestigateDocument> {

  @Override
  public InvestigateDocument withDocumentHeader(DocumentHeader header) {
    return new InvestigateDocument(header, requestHeader(), header.targetId(), header.reviewId(), result());
  }

  @Override
  public InvestigateDocument withRequestHeader(RequestHeader header) {
    return new InvestigateDocument(documentHeader(), header, header.targetId(), header.reviewId(), result());
  }

}
