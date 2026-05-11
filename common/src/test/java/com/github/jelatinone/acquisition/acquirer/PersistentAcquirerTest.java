package com.github.jelatinone.acquisition.acquirer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.acquisition.FetchedBody;
import com.github.jelatinone.acquisition.FetchedMetadata;
import com.github.jelatinone.acquisition.Interpreter;
import com.github.jelatinone.acquisition.interpreter.TextInterpreter;
import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.fixtures.AcquisitionTestFixtures;
import com.github.jelatinone.fixtures.CanonicalTestFixtures;
import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.mock.MockStore;
import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

class PersistentAcquirerTest {

  @Test
  void metadata_mapsFetcherOutput() {
    PersistentCaptureAcquirer acquirer = new PersistentCaptureAcquirer(
        url -> new FetchedBody(CanonicalTestFixtures.url("https://ignored.com"), new byte[0], "ignored",
            MediaType.TEXT_PLAIN,
            MediaEncoding.UTF_8,
            AcquisitionTestFixtures.mediaMetadata("text/plain", 0),
            Instant.now()),
        url -> new FetchedMetadata(CanonicalTestFixtures.url("https://example.com/final"), MediaType.TEXT_HTML,
            MediaEncoding.UTF_8,
            AcquisitionTestFixtures.mediaMetadata("text/html", 25),
            Instant.now()),
        Set.of(),
        new MockStore<>(),
        Duration.ofDays(30L));

    Acquisition.Metadata metadata = acquirer.metadata(new Acquisition.Initial(
        StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID,
        CanonicalTestFixtures.url("https://example.com/start")));

    assertEquals("https://example.com/final", metadata.effectiveUrl().toExternalForm());
    assertEquals(MediaType.TEXT_HTML, metadata.mediaType());
  }

  @Test
  void interpreted_persistsCaptureAndAddsMatchingInterpreterProjection() {
    MockStore<UUID, Capture> store = new MockStore<>();
    PersistentCaptureAcquirer acquirer = new PersistentCaptureAcquirer(
        url -> new FetchedBody(
            CanonicalTestFixtures.url("https://example.com/final"),
            "hello".getBytes(),
            "hash",
            MediaType.TEXT_PLAIN,
            MediaEncoding.UTF_8,
            AcquisitionTestFixtures.mediaMetadata("text/plain", 5),
            Instant.now()),
        url -> new FetchedMetadata(
            CanonicalTestFixtures.url("https://example.com/final"),
            MediaType.TEXT_PLAIN,
            MediaEncoding.UTF_8,
            AcquisitionTestFixtures.mediaMetadata("text/plain", 5),
            Instant.now()),
        Set.<Interpreter<?>>of(new TextInterpreter()),
        store,
        Duration.ofDays(30L));

    Acquisition.Interpreted interpreted = acquirer.interpreted(new Acquisition.Initial(
        StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID,
        CanonicalTestFixtures.url("https://example.com/start")));
    Capture capture = store.query(new Singular<>(Criteria.identifier(StructTestFixtures.TARGET_ID))).orElseThrow();

    assertInstanceOf(Capture.Resolved.class, capture);
    assertEquals("https://example.com/final", interpreted.effectiveUrl().toExternalForm());
    assertEquals(1, interpreted.sourceProjections().size());
    assertNotNull(capture);
    assertTrue(new String(((Capture.Resolved) capture).sourceBytes()).contains("hello"));
  }
}
