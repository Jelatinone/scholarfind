package com.github.jelatinone.meta.archetype.kernel;

import java.time.Instant;

import com.github.jelatinone.model.graph.GraphReviewState;

public interface KernelArchetype {

  Instant reviewableAt(int attempt);

  GraphReviewState.Created create();

  GraphReviewState.Claimed claim(GraphReviewState.Continuable current);

  GraphReviewState.Completed complete(GraphReviewState.Continuable current);

  GraphReviewState.Retry retry(GraphReviewState current, Throwable cause);

  GraphReviewState.Fatal fatal(GraphReviewState current, Throwable cause);
}
