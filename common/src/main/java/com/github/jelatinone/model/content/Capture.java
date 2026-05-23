package com.github.jelatinone.model.content;

import java.net.URL;
import java.time.Instant;

import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

public sealed interface Capture {

  TargetIdentity targetId();

  ReviewIdentity reviewId();

  URL effectiveUrl();

  MediaType mediaType();

  MediaEncoding mediaEncoding();

  MediaMetadata mediaMetadata();

  Instant emittedAt();

  public record Metadata(
      @NonNull TargetIdentity targetId,
      @NonNull ReviewIdentity reviewId,

      @NonNull URL effectiveUrl,

      @NonNull MediaType mediaType,
      @NonNull MediaEncoding mediaEncoding,
      @NonNull MediaMetadata mediaMetadata,

      @NonNull Instant emittedAt) implements Capture {
  }

  public record Resolved(
      @NonNull TargetIdentity targetId,
      @NonNull ReviewIdentity reviewId,

      @NonNull URL effectiveUrl,

      byte[] sourceBytes,
      @NonNull String sourceHash,

      @NonNull MediaType mediaType,
      @NonNull MediaEncoding mediaEncoding,
      @NonNull MediaMetadata mediaMetadata,

      @NonNull Instant emittedAt) implements Capture {

  }

}
