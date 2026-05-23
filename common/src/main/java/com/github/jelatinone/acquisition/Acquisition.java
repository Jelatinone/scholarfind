package com.github.jelatinone.acquisition;

import java.net.URL;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import lombok.NonNull;

public sealed interface Acquisition {

  @NonNull
  TargetIdentity targetId();

  @NonNull
  ReviewIdentity reviewId();

  @NonNull
  URL resolvedUrl();

  @NonNull
  Rank rank();

  public enum Rank {

    INITIAL,

    METADATA,

    INTERPRETED,
  }

  public record Initial(
      @NonNull TargetIdentity targetId,
      @NonNull ReviewIdentity reviewId,

      @NonNull URL canonicalUrl) implements Acquisition {

    @Override
    public URL resolvedUrl() {
      return canonicalUrl();
    }

    @Override
    public Rank rank() {
      return Rank.INITIAL;
    }
  }

  public record Metadata(
      @NonNull TargetIdentity targetId,
      @NonNull ReviewIdentity reviewId,

      @NonNull URL effectiveUrl,

      @NonNull MediaType mediaType,
      @NonNull MediaEncoding mediaEncoding,
      @NonNull MediaMetadata mediaMetadata,

      @NonNull Instant emittedAt) implements Acquisition {

    @Override
    public URL resolvedUrl() {
      return effectiveUrl();
    }

    @Override
    public Rank rank() {
      return Rank.METADATA;
    }
  }

  public record Interpreted(
      @NonNull TargetIdentity targetId,
      @NonNull ReviewIdentity reviewId,

      @NonNull URL effectiveUrl,

      @NonNull MediaType mediaType,
      @NonNull MediaEncoding mediaEncoding,
      @NonNull MediaMetadata mediaMetadata,

      @NonNull Instant emittedAt,

      byte[] sourceBytes,
      Set<Projection> sourceProjections,

      @NonNull String sourceHash

  ) implements Acquisition {

    public Interpreted(
        @NonNull TargetIdentity targetId,
        @NonNull ReviewIdentity reviewId,

        @NonNull URL effectiveUrl,

        @NonNull MediaType mediaType,
        @NonNull MediaEncoding mediaEncoding,
        @NonNull MediaMetadata mediaMetadata,

        @NonNull Instant emittedAt,

        byte[] sourceBytes,

        @NonNull String sourceHash) {
      this(
          targetId,
          reviewId,
          effectiveUrl,
          mediaType,
          mediaEncoding,
          mediaMetadata,
          emittedAt,
          sourceBytes,
          null,
          sourceHash);
    }

    public Interpreted {
      sourceProjections = sourceProjections == null ? Set.of() : Set.copyOf(sourceProjections);
    }

    public Interpreted withProjection(@NonNull Projection sourceProjection) {
      Set<Projection> updatedProjections = new HashSet<>(sourceProjections());
      updatedProjections.add(sourceProjection);
      return new Interpreted(
          targetId(),
          reviewId(),
          effectiveUrl(),
          mediaType(),
          mediaEncoding(),
          mediaMetadata(),
          emittedAt(),
          sourceBytes(),
          updatedProjections,
          sourceHash());
    }

    @Override
    public URL resolvedUrl() {
      return effectiveUrl();
    }

    @Override
    public Rank rank() {
      return Rank.INTERPRETED;
    }
  }
}
