package com.github.jelatinone.acquisition;

import java.net.URL;
import java.time.Instant;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;

import lombok.NonNull;

public record FetchedMetadata(
    @NonNull URL effectiveUrl,

    @NonNull MediaType mediaType,
    @NonNull MediaEncoding mediaEncoding,
    @NonNull MediaMetadata mediaMetadata,
    @NonNull Instant fetchedAt) {

}
