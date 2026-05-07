package com.github.jelatinone.model.annotate;

import java.util.UUID;

import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;

import lombok.NonNull;

public record AnnotateRequest(
    @NonNull RequestHeader requestHeader,
    @NonNull UUID targetId,
    @NonNull UUID reviewId,
    Classification classification) implements Request {
}
