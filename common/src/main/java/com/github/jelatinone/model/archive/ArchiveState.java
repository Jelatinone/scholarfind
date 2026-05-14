package com.github.jelatinone.model.archive;

import java.time.Instant;

import com.github.jelatinone.model.audit.ExecutionStage;

import lombok.NonNull;

public sealed interface ArchiveState {

  @NonNull
  ExecutionStage reviewedBy();

  @NonNull
  String reviewerName();

  @NonNull
  Instant reviewedAt();

  record Pending(@NonNull ExecutionStage reviewedBy, @NonNull String reviewerName, @NonNull Instant reviewedAt)
      implements ArchiveState {
  }

  record Active(@NonNull ExecutionStage reviewedBy, @NonNull String reviewerName, @NonNull Instant reviewedAt)
      implements ArchiveState {
  }

  record Expired(@NonNull ExecutionStage reviewedBy, @NonNull String reviewerName, @NonNull Instant reviewedAt)
      implements ArchiveState {
  }
}
