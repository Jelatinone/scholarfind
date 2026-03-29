package com.github.scholarfind.task;

import java.time.Instant;

import com.github.scholarfind.models.ingest.IngestDecision;

public record IngestState(
    Instant reviewedAt,
    IngestDecision decision,
    Integer depthBudget) {

  public static IngestState initial(Instant reviewedAt, Integer depthBudget) {
    return new IngestState(reviewedAt, IngestDecision.PENDING, depthBudget);
  }

  public IngestState withDecision(IngestDecision nextDecision) {
    return new IngestState(reviewedAt, nextDecision, depthBudget);
  }

  public IngestState withDepthBudget(Integer nextDepthBudget) {
    return new IngestState(reviewedAt, decision, nextDepthBudget);
  }
}
