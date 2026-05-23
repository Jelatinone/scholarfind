package com.github.jelatinone.meta.archetype.kernel;

import java.util.Collection;
import java.util.List;

import com.github.jelatinone.model.graph.GraphNode;
import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public record KernelResult<Emit extends Request<Emit>, Node extends GraphNode<?>>(
    @NonNull KernelOperand<Node> operable,
    Collection<@NonNull Request<Emit>> letters) {

  public KernelResult {
    letters = List.copyOf(letters);
  }
}
