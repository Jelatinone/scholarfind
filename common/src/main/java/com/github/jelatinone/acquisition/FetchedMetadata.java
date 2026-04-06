package com.github.jelatinone.acquisition;

import java.net.URL;

public record FetchedMetadata(
    URL effectiveUrl,
    Integer statusCode,
    Integer redirectHopCount,
    Integer setCookieCount,
    String contentTypeHeader,
    Long contentLength) {
}
