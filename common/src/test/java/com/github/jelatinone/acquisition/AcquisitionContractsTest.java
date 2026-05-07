package com.github.jelatinone.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.AcquisitionTestFixtures;
import com.github.jelatinone.fixtures.CanonicalTestFixtures;
import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

class AcquisitionContractsTest {

	@Test
	void interpreterHelpers_decodeAndBoundText() {
		assertEquals("hello", Interpreter.decode("hello".getBytes(), MediaEncoding.UTF_8));
		assertEquals("trimmed", Interpreter.bound("  trimmed  ", 20));
		assertEquals("trim", Interpreter.bound("trimmed", 4));
	}

	@Test
	void acquisitions_preserveResolvedUrlAndAppendProjections() {
		Acquisition.Initial initial = new Acquisition.Initial(
				StructTestFixtures.TARGET_ID,
				StructTestFixtures.REVIEW_ID,
				CanonicalTestFixtures.url("https://example.com/start"));
		Acquisition.Metadata metadata = new Acquisition.Metadata(
				StructTestFixtures.TARGET_ID,
				StructTestFixtures.REVIEW_ID,
				CanonicalTestFixtures.url("https://example.com/final"),
				MediaType.TEXT_HTML,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("text/html", 10),
				StructTestFixtures.NOW);
		Acquisition.Interpreted interpreted = AcquisitionTestFixtures.acquisition(
				"<html>Hello</html>".getBytes(),
				MediaType.TEXT_HTML,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("text/html", 18));
		Projection.Normalized projection = new Projection.Normalized(MediaType.TEXT_HTML, "Hello");

		Acquisition.Interpreted withProjection = interpreted.withProjection(projection);

		assertEquals(initial.canonicalUrl(), initial.resolvedUrl());
		assertEquals(metadata.effectiveUrl(), metadata.resolvedUrl());
		assertNotSame(interpreted, withProjection);
		assertEquals(1, withProjection.sourceProjections().size());
		assertSame(projection, withProjection.sourceProjections().iterator().next());
	}

	@Test
	void fetchedRecords_retainResolvedContentFields() {
		FetchedMetadata metadata = new FetchedMetadata(
				CanonicalTestFixtures.url("https://example.com/final"),
				MediaType.TEXT_PLAIN,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("text/plain", 5));
		FetchedBody body = new FetchedBody(
				CanonicalTestFixtures.url("https://example.com/final"),
				"hello".getBytes(),
				"hash",
				MediaType.TEXT_PLAIN,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("text/plain", 5));

		assertEquals(MediaType.TEXT_PLAIN, metadata.mediaType());
		assertEquals("hash", body.sourceHash());
		assertEquals(5L, body.mediaMetadata().contentLength());
	}
}
