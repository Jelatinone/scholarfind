package com.github.jelatinone.model.publication;

import java.time.Instant;

import com.github.jelatinone.model.audit.ExecutionStage;

import lombok.NonNull;

public sealed interface PublicationState {

  @NonNull
  ExecutionStage reviewedBy();

  @NonNull
  String reviewerName();

  @NonNull
  Instant reviewedAt();

  record Pending(@NonNull ExecutionStage reviewedBy, @NonNull String reviewerName, @NonNull Instant reviewedAt)
      implements PublicationState {
  }

  record Active(@NonNull ExecutionStage reviewedBy, @NonNull String reviewerName, @NonNull Instant reviewedAt)
      implements PublicationState {
  }

  record Expired(@NonNull ExecutionStage reviewedBy, @NonNull String reviewerName, @NonNull Instant reviewedAt)
      implements PublicationState {
  }
}
