package com.github.jelatinone.model.graph;

import java.time.Instant;

import com.github.jelatinone.model.audit.ExecutionStage;

import lombok.NonNull;

public sealed interface GraphReviewState
    permits GraphReviewState.Continuable, GraphReviewState.Terminal {

  @NonNull
  ExecutionStage reviewedBy();

  @NonNull
  String reviewerName();

  @NonNull
  Instant reviewedAt();

  int attempts();

  public sealed interface Continuable extends GraphReviewState permits Created, Claimed {

    @NonNull
    Instant reviewableAt();
  }

  public sealed interface Terminal extends GraphReviewState permits Fatal, Retry, Completed {

    @NonNull
    String cause();
  }

  public record Created(
      @NonNull ExecutionStage reviewedBy,
      @NonNull String reviewerName,
      @NonNull Instant reviewedAt,
      @NonNull Instant reviewableAt)
      implements Continuable {

    public Created(ExecutionStage reviewedBy, String reviewerName, Instant reviewedAt) {
      this(reviewedBy, reviewerName, reviewedAt, reviewedAt);
    }

    @Override
    public int attempts() {
      return 0;
    }
  }

  public record Claimed(
      @NonNull ExecutionStage reviewedBy,
      @NonNull String reviewerName,
      @NonNull Instant reviewedAt,
      @NonNull Instant reviewableAt,
      int attempts)
      implements Continuable {
  }

  public record Completed(
      @NonNull ExecutionStage reviewedBy,
      @NonNull String reviewerName,
      @NonNull Instant reviewedAt,
      @NonNull String cause,
      int attempts)
      implements Terminal {
  }

  public record Fatal(
      @NonNull ExecutionStage reviewedBy,
      @NonNull String reviewerName,
      @NonNull Instant reviewedAt,
      @NonNull String cause,
      int attempts)
      implements Terminal {
  }

  public record Retry(
      @NonNull ExecutionStage reviewedBy,
      @NonNull String reviewerName,
      @NonNull Instant reviewedAt,
      @NonNull String cause,
      int attempts)
      implements Terminal {
  }
}
