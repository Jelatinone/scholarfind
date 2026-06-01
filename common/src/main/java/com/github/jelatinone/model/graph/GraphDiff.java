package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.Set;

import com.github.jelatinone.model.struct.Identity.DiffIdentity;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;

public interface GraphDiff {

  DiffIdentity diffId();

  ReviewIdentity reviewId();

  Instant emittedAt();

  public record Unversioned(
      DiffIdentity diffId,
      ReviewIdentity reviewId,

      Instant emittedAt) implements GraphDiff {
  }

  public record Modified(
      DiffIdentity diffId,
      ReviewIdentity reviewId,

      Set<Change> changes,

      Instant emittedAt) implements GraphDiff {
  }

  public record Change(String field, Object before, Object after) {
  }
}
