package com.github.jelatinone.model.graph;

import com.github.jelatinone.model.audit.ExecutionStage;

import lombok.NonNull;

public sealed interface TargetState {

  @NonNull
  ExecutionStage reviewedBy();

  @NonNull
  String reviewerName();

  public record Created(ExecutionStage reviewedBy, String reviewerName) implements TargetState {
  }

  public record Claimed(ExecutionStage reviewedBy, String reviewerName) implements TargetState {
  }

  public record Completed(ExecutionStage reviewedBy, String reviewerName) implements TargetState {
  }

  public record Fatal(ExecutionStage reviewedBy, String reviewerName, Throwable cause) implements TargetState {
  }

  public record Retry(ExecutionStage reviewedBy, String reviewerName, Throwable cause) implements TargetState {
  }
}
