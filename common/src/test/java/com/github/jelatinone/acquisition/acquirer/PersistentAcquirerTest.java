package com.github.jelatinone.acquisition.acquirer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.acquisition.FetchedBody;
import com.github.jelatinone.acquisition.FetchedMetadata;
import com.github.jelatinone.acquisition.Interpreter;
import com.github.jelatinone.acquisition.interpreter.TextInterpreter;
import com.github.jelatinone.fixtures.Tests;
import com.github.jelatinone.infra.mock.MockStore;
import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

class PersistentAcquirerTest {

	@Test
	void metadata_mapsFetcherOutput() {
		PersistentAcquirer acquirer = new PersistentAcquirer(
				url -> new FetchedBody(Tests.url("https://ignored.com"), new byte[0], "ignored", MediaType.TEXT_PLAIN,
						MediaEncoding.UTF_8,
						Tests.mediaMetadata("text/plain", 0)),
				url -> new FetchedMetadata(Tests.url("https://example.com/final"), MediaType.TEXT_HTML,
						MediaEncoding.UTF_8,
						Tests.mediaMetadata("text/html", 25)),
				Set.of(),
				new MockStore<>());

		Acquisition.Metadata metadata = acquirer.metadata(new Acquisition.Initial(
				Tests.TARGET_ID,
				Tests.REVIEW_ID,
				Tests.url("https://example.com/start")));

		assertEquals("https://example.com/final", metadata.effectiveUrl().toExternalForm());
		assertEquals(MediaType.TEXT_HTML, metadata.mediaType());
	}

	@Test
	void interpreted_persistsCaptureAndAddsMatchingInterpreterProjection() {
		MockStore<Capture, UUID> store = new MockStore<>();
		PersistentAcquirer acquirer = new PersistentAcquirer(
				url -> new FetchedBody(
						Tests.url("https://example.com/final"),
						"hello".getBytes(),
						"hash",
						MediaType.TEXT_PLAIN,
						MediaEncoding.UTF_8,
						Tests.mediaMetadata("text/plain", 5)),
				url -> new FetchedMetadata(
						Tests.url("https://example.com/final"),
						MediaType.TEXT_PLAIN,
						MediaEncoding.UTF_8,
						Tests.mediaMetadata("text/plain", 5)),
				Set.<Interpreter<?>>of(new TextInterpreter()),
				store);

		Acquisition.Interpreted interpreted = acquirer.interpreted(new Acquisition.Initial(
				Tests.TARGET_ID,
				Tests.REVIEW_ID,
				Tests.url("https://example.com/start")));
		Capture capture = store.get(Tests.TARGET_ID);

		assertEquals("https://example.com/final", interpreted.effectiveUrl().toExternalForm());
		assertEquals(1, interpreted.sourceProjections().size());
		assertNotNull(capture);
		assertTrue(new String(capture.sourceBytes()).contains("hello"));
	}
}
