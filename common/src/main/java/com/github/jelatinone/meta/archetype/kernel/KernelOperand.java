package com.github.jelatinone.meta.archetype.kernel;

import com.github.jelatinone.model.graph.GraphNode;
import com.github.jelatinone.model.graph.GraphReview;

import lombok.NonNull;

public sealed interface KernelOperand<Node extends GraphNode<?>> {

  @NonNull
  Node target();

  public record Unreviewed<Node extends GraphNode<?>>(@NonNull Node target) implements KernelOperand<Node> {
  }

  public record Reviewed<Node extends GraphNode<?>>(@NonNull Node target, @NonNull GraphReview review)
      implements KernelOperand<Node> {
    public Reviewed {
      if (!target.canonicalId().equals(review.targetId())) {
        throw new IllegalArgumentException("Target work must reference the same target as its review");
      }
    }
  }
}
