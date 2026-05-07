package com.github.jelatinone.acquisition.acquirer;

import java.net.URL;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.acquisition.Acquirer;
import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.acquisition.BodyFetcher;
import com.github.jelatinone.acquisition.FetchedBody;
import com.github.jelatinone.acquisition.FetchedMetadata;
import com.github.jelatinone.acquisition.Interpreter;
import com.github.jelatinone.acquisition.MetadataFetcher;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.acquisition.Acquisition.Interpreted;
import com.github.jelatinone.acquisition.Acquisition.Metadata;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PersistentAcquirer implements Acquirer {

	BodyFetcher bodyFetcher;
	MetadataFetcher metadataFetcher;

	Set<Interpreter<?>> interpreters;

	Store<Capture, UUID> captureStore;

	@Override
	public Metadata metadata(@NonNull Acquisition current) {
		URL resolvedUrl = current.resolvedUrl();
		FetchedMetadata fetchedMetadata = metadataFetcher.fetchMetadata(resolvedUrl);

		Metadata acquisition = new Metadata(
				current.targetId(),
				current.reviewId(),
				fetchedMetadata.effectiveUrl(),
				fetchedMetadata.mediaType(),
				fetchedMetadata.mediaEncoding(),
				fetchedMetadata.mediaMetadata(),
				Instant.now());
		return acquisition;
	}

	@Override
	public Interpreted interpreted(@NonNull Acquisition current) {
		URL resolvedUrl = current.resolvedUrl();
		FetchedBody fetchedBody = bodyFetcher.fetchBody(resolvedUrl);

		Instant fetchedAt = Instant.now();

		Capture capture = new Capture(
				current.targetId(),
				current.reviewId(),
				fetchedBody.sourceBytes(),
				fetchedAt);
		captureStore.put(current.targetId(), capture);

		Interpreted uninterpreted = new Interpreted(
				current.targetId(),
				current.reviewId(),
				fetchedBody.effectiveUrl(),
				fetchedBody.mediaType(),
				fetchedBody.mediaEncoding(),
				fetchedBody.mediaMetadata(),
				fetchedAt,
				fetchedBody.sourceBytes(),
				fetchedBody.sourceHash());
		Interpreted interpreted = interpreters.stream()
				.filter((interpreter) -> interpreter.supports(fetchedBody.mediaType()))
				.map((interpreter) -> interpreter.interpret(uninterpreted))
				.reduce(
						uninterpreted,
						(acquisition, projection) -> acquisition.withProjection(projection),
						(left, right) -> right);

		return interpreted;
	}

}
