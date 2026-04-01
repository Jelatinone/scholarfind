package com.github.jelatinone.models.ingest;

public enum IngestDecision {
  PENDING,
  ADMITTED,
  DUPLICATE_SUPPRESSED,
  DEPTH_EXCEEDED,
  INVALID_TARGET
}
