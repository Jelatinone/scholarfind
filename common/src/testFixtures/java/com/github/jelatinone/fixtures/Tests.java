package com.github.jelatinone.fixtures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Letter;

public final class Tests {

	public static final UUID TARGET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	public static final UUID REVIEW_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	public static final Instant NOW = Instant.parse("2026-05-07T12:00:00Z");

	private Tests() {
	}

	public static URL url(String value) {
		try {
			return URI.create(value).toURL();
		} catch (MalformedURLException exception) {
			throw new IllegalArgumentException(exception);
		}
	}

	public static RequestHeader requestHeader(int attempt, ExecutionStage emittedBy) {
		return new RequestHeader(TARGET_ID, REVIEW_ID, attempt, emittedBy, NOW);
	}

	public static DocumentHeader documentHeader(ExecutionStage emittedBy) {
		return new DocumentHeader(TARGET_ID, REVIEW_ID, emittedBy, NOW);
	}

	public static InvestigateRequest request(int attempt, ExecutionStage emittedBy) {
		return new InvestigateRequest(requestHeader(attempt, emittedBy), TARGET_ID, REVIEW_ID);
	}

	public static Classification.Interpreted classification() {
		return new Classification.Interpreted(
				Map.of(Category.LANDING, 0.95d),
				0.95d,
				Set.of(Category.LANDING),
				Category.LANDING,
				true);
	}

	public static InvestigateDocument document(int attempt, ExecutionStage emittedBy) {
		return new InvestigateDocument(
				documentHeader(emittedBy),
				requestHeader(attempt, emittedBy),
				TARGET_ID,
				REVIEW_ID,
				classification(),
				Set.of(UUID.fromString("33333333-3333-3333-3333-333333333333")));
	}

	public static Letter<InvestigateRequest> letter(int attempt, ExecutionStage executionRef) {
		InvestigateRequest request = request(attempt, executionRef);
		return new Letter<>(
				request.targetId(),
				request.reviewId(),
				executionRef,
				request,
				NOW);
	}

	public static MediaMetadata mediaMetadata(String contentType, long contentLength) {
		return new MediaMetadata(200, 0, 0, contentType, contentLength);
	}

	public static Acquisition.Interpreted acquisition(
			byte[] sourceBytes,
			MediaType mediaType,
			MediaEncoding mediaEncoding,
			MediaMetadata mediaMetadata) {
		return new com.github.jelatinone.acquisition.Acquisition.Interpreted(
				TARGET_ID,
				REVIEW_ID,
				url("https://example.com/content"),
				mediaType,
				mediaEncoding,
				mediaMetadata,
				NOW,
				sourceBytes,
				"source-hash");
	}
}
