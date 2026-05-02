package com.github.jelatinone.model.investigate;

import java.util.UUID;

import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;

public record InvestigateRequest(
		RequestHeader requestHeader,

		UUID targetId,
		UUID reviewId

) implements Request {

}
