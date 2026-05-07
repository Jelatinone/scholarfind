package com.github.jelatinone.acquisition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.content.CaptureReference;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.struct.RequestHeader;

final class PersistentAcquisitionServiceTest {
  private static final Instant NOW = Instant.parse("2026-05-05T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void ensureMetadataPersistsContentDocumentWithDetectedMetadata() throws Exception {
    URL url = URI.create("https://example.com/scholarship").toURL();
    UUID targetId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    MemoryStore<ContentDocument, UUID> contentStore = new MemoryStore<>(ContentDocument::targetId);

    PersistentAcquisitionService service = service(
        fetchedMetadata(url),
        null,
        contentStore,
        byteStore(),
        byteStore());

    AcquiredContent acquired = service.ensureMetadata(content(targetId, reviewId, url, null));

    assertTrue(acquired.hasMetadata());
    assertEquals(MediaType.TEXT_HTML, acquired.mediaType());
    assertEquals(MediaEncoding.UTF_8, acquired.mediaEncoding());
    assertEquals(NOW, acquired.contentDocument().documentHeader().emittedAt());
    assertEquals(acquired.contentDocument(), contentStore.get(targetId));
  }

  @Test
  void ensureContentFetchesStoresAndHydratesText() throws Exception {
    URL url = URI.create("https://example.com/scholarship.txt").toURL();
    UUID targetId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    byte[] body = "hello scholarship world".getBytes(StandardCharsets.UTF_8);
    MemoryStore<ContentDocument, UUID> contentStore = new MemoryStore<>(ContentDocument::targetId);
    MemoryStore<byte[], CaptureReference> sourceStore = byteStore();
    MemoryStore<byte[], CaptureReference> textStore = byteStore();

    PersistentAcquisitionService service = service(
        fetchedMetadata(url),
        new FetchedBody(
            new FetchedMetadata(url, 200, 0, 0, "text/plain; charset=utf-8", (long) body.length),
            body),
        contentStore,
        sourceStore,
        textStore);

    AcquiredContent acquired = service.ensureContent(content(targetId, reviewId, url, null));

    assertTrue(acquired.hasHydratedSource());
    assertTrue(acquired.hasCompleteText());
    assertArrayEquals(body, sourceStore.get(acquired.capture().sourceCapture()));
    assertEquals("hello scholarship world", acquired.hydratedText());
    assertEquals(acquired.contentDocument(), contentStore.get(targetId));
  }

  private static PersistentAcquisitionService service(
      FetchedMetadata metadata,
      FetchedBody body,
      Store<ContentDocument, UUID> contentStore,
      Store<byte[], CaptureReference> sourceStore,
      Store<byte[], CaptureReference> textStore) {
    MetadataFetcher metadataFetcher = ignored -> metadata;
    BodyFetcher bodyFetcher = ignored -> body;
    return new PersistentAcquisitionService(
        metadataFetcher,
        bodyFetcher,
        new ContentDetector(),
        CLOCK,
        List.of(new com.github.jelatinone.acquisition.interpreter.TextContentInterpreter()),
        contentStore,
        sourceStore,
        textStore);
  }

  private static FetchedMetadata fetchedMetadata(URL url) {
    return new FetchedMetadata(url, 200, 0, 0, "text/html; charset=utf-8", 100L);
  }

  private static AcquiredContent content(
      UUID targetId,
      UUID reviewId,
      URL url,
      ContentDocument document) {
    RequestHeader header = new RequestHeader(targetId, reviewId, 0, ExecutionStage.INVESTIGATE, NOW);
    return new AcquiredContent(
        header,
        new TargetNode(targetId, url, NOW),
        reviewId,
        document,
        null,
        null,
        false);
  }

  private static MemoryStore<byte[], CaptureReference> byteStore() {
    return new MemoryStore<>(bytes -> new CaptureReference(
        new UUID(0L, 0L),
        hash(bytes),
        hash(bytes),
        NOW));
  }

  private static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static final class MemoryStore<Body, Key> implements Store<Body, Key> {
    private final Function<Body, Key> keyExtractor;
    private final Map<Key, Body> values = new HashMap<>();

    private MemoryStore(Function<Body, Key> keyExtractor) {
      this.keyExtractor = keyExtractor;
    }

    @Override
    public Key put(Body body) {
      Key key = keyExtractor.apply(body);
      values.put(key, body);
      return key;
    }

    @Override
    public Body get(Key key) {
      return values.get(key);
    }

    @Override
    public void delete(Key key) {
      values.remove(key);
    }

    @Override
    public void close() {
    }
  }
}
