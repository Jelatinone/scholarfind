package com.github.jelatinone.meta.archetype.kernel;

import com.github.jelatinone.model.graph.GraphNode;
import com.github.jelatinone.model.graph.GraphReview;

import lombok.NonNull;

public sealed interface KernelOperand {

	@NonNull
	GraphNode target();

	public record Unreviewed(@NonNull GraphNode target) implements KernelOperand {
	}

	public record Reviewed(@NonNull GraphNode target, @NonNull GraphReview review) implements KernelOperand {
		public Reviewed {
			if (!target.canonicalId().equals(review.targetId())) {
				throw new IllegalArgumentException("Target work must reference the same target as its review");
			}
		}
	}
}
