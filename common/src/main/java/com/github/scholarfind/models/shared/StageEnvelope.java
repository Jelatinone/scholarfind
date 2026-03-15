package com.github.scholarfind.models.shared;

import java.util.UUID;

import com.github.scholarfind.models.audit.ProcessingStage;

public record StageEnvelope<R extends Request>(
    long schemaVersion,
    ProcessingStage stage,
    String executionRef,
    UUID requestId,
    UUID targetId,
    R payload) {

  public static final long schemaVersionValue = 1L;

  public static <T extends Request> StageEnvelope<T> of(
      ProcessingStage stage,
      String executionRef,
      T payload) {
    return new StageEnvelope<>(
        schemaVersionValue,
        stage,
        executionRef,
        payload.requestHeader().requestId(),
        payload.target().targetId(),
        payload);
  }
}
