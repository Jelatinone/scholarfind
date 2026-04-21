package com.github.jelatinone.model.struct;

import java.util.UUID;

public interface Document<Self> {

	DocumentHeader documentHeader();

	RequestHeader requestHeader();

	UUID targetId();

	UUID reviewId();

	Self withDocumentHeader(DocumentHeader header);

	Self withRequestHeader(RequestHeader header);

}
