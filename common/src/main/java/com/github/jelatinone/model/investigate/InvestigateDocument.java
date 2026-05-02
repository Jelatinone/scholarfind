package com.github.jelatinone.model.investigate;

import java.util.UUID;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;

public record InvestigateDocument(
		DocumentHeader documentHeader,
		RequestHeader requestHeader,

		UUID targetId,
		UUID reviewId,

		Classification classification

) implements Document<InvestigateDocument> {

	@Override
	public InvestigateDocument withDocumentHeader(DocumentHeader header) {
		return new InvestigateDocument(header, requestHeader(), targetId(), reviewId(), classification());
	}

	@Override
	public InvestigateDocument withRequestHeader(RequestHeader header) {
		return new InvestigateDocument(documentHeader(), header, targetId(), reviewId(), classification());
	}

}
