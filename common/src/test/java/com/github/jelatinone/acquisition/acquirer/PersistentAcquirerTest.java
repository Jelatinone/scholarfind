package com.github.jelatinone.acquisition.acquirer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
    MockStore<UUID, Capture> store = new MockStore<>();
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
        store,
        Duration.ofDays(30L));

    Acquisition.Metadata metadata = acquirer.metadata(new Acquisition.Initial(
        StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID,
        CanonicalTestFixtures.url("https://example.com/start")));

    assertEquals("https://example.com/final", metadata.effectiveUrl().toExternalForm());
    assertEquals(MediaType.TEXT_HTML, metadata.mediaType());

    Capture.Metadata capture = assertInstanceOf(Capture.Metadata.class, store.get(StructTestFixtures.TARGET_ID));
    assertEquals("https://example.com/final", capture.effectiveUrl().toExternalForm());
    assertEquals(metadata.emittedAt(), capture.emittedAt());
  }

  @Test
  void metadata_reusesFreshCapture_andRefreshesStaleCapture() {
    Capture.Metadata freshCapture = new Capture.Metadata(
        StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID,
        CanonicalTestFixtures.url("https://example.com/fresh"),
        MediaType.TEXT_HTML,
        MediaEncoding.UTF_8,
        AcquisitionTestFixtures.mediaMetadata("text/html", 5),
        Instant.now());
    MockStore<UUID, Capture> store = new MockStore<>(Map.of(StructTestFixtures.TARGET_ID, freshCapture));
    AtomicInteger metadataFetches = new AtomicInteger();
    PersistentCaptureAcquirer acquirer = new PersistentCaptureAcquirer(
        url -> new FetchedBody(CanonicalTestFixtures.url("https://ignored.com"), new byte[0], "ignored",
            MediaType.TEXT_PLAIN,
            MediaEncoding.UTF_8,
            AcquisitionTestFixtures.mediaMetadata("text/plain", 0),
            Instant.now()),
        url -> {
          metadataFetches.incrementAndGet();
          return new FetchedMetadata(CanonicalTestFixtures.url("https://example.com/refetched"), MediaType.TEXT_PLAIN,
              MediaEncoding.UTF_8,
              AcquisitionTestFixtures.mediaMetadata("text/plain", 10),
              Instant.now());
        },
        Set.of(),
        store,
        Duration.ofDays(30L));
    Acquisition.Initial request = new Acquisition.Initial(
        StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID,
        CanonicalTestFixtures.url("https://example.com/start"));

    Acquisition.Metadata cached = acquirer.metadata(request);
    store.put(StructTestFixtures.TARGET_ID, new Capture.Metadata(
        StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID,
        CanonicalTestFixtures.url("https://example.com/stale"),
        MediaType.TEXT_HTML,
        MediaEncoding.UTF_8,
        AcquisitionTestFixtures.mediaMetadata("text/html", 5),
        Instant.now().minus(Duration.ofDays(31L))));
    Acquisition.Metadata refreshed = acquirer.metadata(request);

    assertEquals("https://example.com/fresh", cached.effectiveUrl().toExternalForm());
    assertEquals("https://example.com/refetched", refreshed.effectiveUrl().toExternalForm());
    assertEquals(1, metadataFetches.get());
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
    assertEquals("https://example.com/final", capture.effectiveUrl().toExternalForm());
    assertTrue(new String(((Capture.Resolved) capture).sourceBytes()).contains("hello"));
  }
}
