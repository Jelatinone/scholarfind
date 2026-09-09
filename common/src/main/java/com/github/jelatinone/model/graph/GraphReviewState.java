package com.github.jelatinone.model.graph;

import java.time.Instant;
import java.util.List;

import com.github.jelatinone.model.audit.AttemptEvent;

import lombok.NonNull;

public sealed interface GraphReviewState
		permits GraphReviewState.Continuable, GraphReviewState.Terminal {

	@NonNull
	List<AttemptEvent> events();

	// TODO: Create a seperate emissions type which uses a sealed interface

	public sealed interface Continuable extends GraphReviewState permits Available, Claimed {

		@NonNull
		Instant reviewableAt();
	}

	public sealed interface Terminal extends GraphReviewState permits Fatal, Retry, Completed {
	}

	public record Available(
			@NonNull Instant reviewableAt,
			@NonNull List<AttemptEvent> events)
			implements Continuable {

		public Available(Instant reviewedAt) {
			this(reviewedAt, List.of());
		}
	}

	public record Claimed(
			@NonNull Instant reviewableAt,
			@NonNull List<AttemptEvent> events)
			implements Continuable {
	}

	public record Completed(
			@NonNull List<AttemptEvent> events)
			implements Terminal {
	}

	public record Fatal(
			@NonNull List<AttemptEvent> events)
			implements Terminal {
	}

	public record Retry(
			@NonNull Instant reviewableAt,
			@NonNull List<AttemptEvent> events)
			implements Terminal {
	}
}
