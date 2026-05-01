package com.github.jelatinone.model.content;

public record MediaMetadata(
		Integer statusCode,
		Integer redirectCount,
		Integer cookieCount,

		Long contentLength) {

}
