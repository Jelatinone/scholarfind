package com.github.jelatinone.acquisition.acquirer;

import java.net.URL;
import java.time.Duration;
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
import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Singular;
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
public final class PersistentCaptureAcquirer implements Acquirer {

	BodyFetcher bodyFetcher;
	MetadataFetcher metadataFetcher;

	Set<Interpreter<?>> interpreters;

	Store<Capture, UUID> captureStore;
	Duration captureStalenessTimeout;

	@Override
	public Metadata metadata(@NonNull Acquisition current) {
		Capture storedCapture = captureStore.query(new Singular<Criteria<UUID>>(Criteria.<UUID>id(current.targetId())));
		if (storedCapture != null && storedCapture.emittedAt().minus(captureStalenessTimeout).isBefore(Instant.now())) {
			Metadata metadata = new Metadata(
					storedCapture.targetId(),
					storedCapture.reviewId(),
					storedCapture.effectiveUrl(),
					storedCapture.mediaType(),
					storedCapture.mediaEncoding(),
					storedCapture.mediaMetadata(),
					storedCapture.emittedAt());
			return metadata;
		}

		URL resolvedUrl = current.resolvedUrl();
		FetchedMetadata fetchedMetadata = metadataFetcher.fetchMetadata(resolvedUrl);
		Instant fetchedAt = Instant.now();

		Capture capture = new Capture.Metadata(
				current.targetId(),
				current.reviewId(),
				resolvedUrl,
				fetchedMetadata.mediaType(),
				fetchedMetadata.mediaEncoding(),
				fetchedMetadata.mediaMetadata(),
				fetchedAt);
		captureStore.put(current.targetId(), capture);

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
		Capture storedCapture = captureStore.query(new Singular<Criteria<UUID>>(Criteria.<UUID>id(current.targetId())));
		if (storedCapture != null && storedCapture.emittedAt().minus(captureStalenessTimeout).isBefore(Instant.now())) {
			switch (storedCapture) {
				case Capture.Resolved resolved -> {
					Interpreted interpreted = new Interpreted(
							resolved.targetId(),
							resolved.reviewId(),
							resolved.effectiveUrl(),
							resolved.mediaType(),
							resolved.mediaEncoding(),
							resolved.mediaMetadata(),
							resolved.emittedAt(),
							resolved.sourceBytes(),
							resolved.sourceHash());
					return interpreted;
				}

				default -> {
				}
			}
		}
		URL resolvedUrl = current.resolvedUrl();

		FetchedBody fetchedBody = bodyFetcher.fetchBody(resolvedUrl);
		Instant fetchedAt = Instant.now();

		Capture capture = new Capture.Resolved(
				current.targetId(),
				current.reviewId(),
				resolvedUrl,
				fetchedBody.sourceBytes(),
				fetchedBody.sourceHash(),
				fetchedBody.mediaType(),
				fetchedBody.mediaEncoding(),
				fetchedBody.mediaMetadata(),
				fetchedAt);
		captureStore.put(current.targetId(), capture);

		Interpreted interpreted = new Interpreted(
				current.targetId(),
				current.reviewId(),
				fetchedBody.effectiveUrl(),
				fetchedBody.mediaType(),
				fetchedBody.mediaEncoding(),
				fetchedBody.mediaMetadata(),
				fetchedAt,
				fetchedBody.sourceBytes(),
				fetchedBody.sourceHash());
		return interpreters.stream()
				.filter((interpreter) -> interpreter.supports(fetchedBody.mediaType()))
				.map((interpreter) -> interpreter.interpret(interpreted))
				.reduce(
						interpreted,
						(acquisition, projection) -> acquisition.withProjection(projection),
						(left, right) -> right);
	}
}
