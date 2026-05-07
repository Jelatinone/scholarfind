package com.github.jelatinone.model.content;

public record MediaMetadata(
		Integer statusCode,
		Integer redirectCount,
		Integer cookieCount,

		String contentHeader,

		Long contentLength) {

}
