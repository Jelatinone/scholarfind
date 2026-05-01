package com.github.jelatinone.model.content;

public record MediaMetadata(
		int statusCode,
		int redirectCount,
		int cookieCount,

		long contentLength) {

}
