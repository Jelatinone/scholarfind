package com.github.jelatinone.acquisition;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.model.content.CaptureReference;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.model.struct.DocumentHeader;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcquisitionService {
  MetadataFetcher metadataFetcher;
  BodyFetcher bodyFetcher;

  List<ContentInterpreter> interpreters;

  Store<ContentDocument, UUID> contentStore;
  Store<byte[], CaptureReference> sourceCaptureStore;
  Store<byte[], CaptureReference> normalizedCaptureStore;

  public AcquiredContent ensureMetadata(@NonNull AcquiredContent current) {
    if (current.hasMetadata()) {
      return current;
    }

    FetchedMetadata fetched = metadataFetcher.fetch(targetUrl(current));
    DetectedContent detected = detect(fetched.contentTypeHeader());
    Capture capture = mergeCapture(
        current,
        fetched.effectiveUrl(),
        detected,
        new MediaMetadata(
            fetched.statusCode(),
            fetched.redirectHopCount(),
            fetched.setCookieCount(),
            fetched.contentLength()),
        null,
        null);
    return current.withContentDocument(persist(current, capture, null));
  }

  public AcquiredContent ensureContent(@NonNull AcquiredContent current) {
    AcquiredContent withMetadata = current.hasMetadata()
        ? current
        : ensureMetadata(current);
    if (withMetadata.hasHydratedSource()) {
      return withMetadata;
    }
    if (withMetadata.hasSourceSnapshot()) {
      byte[] source = sourceCaptureStore.get(withMetadata.capture().sourceCapture());
      Capture capture = withMetadata.contentLength() == null
          ? withContentLength(withMetadata.capture(), (long) source.length)
          : withMetadata.capture();
      AcquiredContent hydrated = withMetadata.withHydratedSource(source);
      if (capture != withMetadata.capture()) {
        hydrated = hydrated.withContentDocument(persist(hydrated, capture, null));
      }
      return hydrated;
    }

    FetchedBody fetched = bodyFetcher.fetch(targetUrl(withMetadata));
    DetectedContent detected = detect(fetched.metadata().contentTypeHeader());
    InterpretedContent interpreted = interpret(fetched.body(), detected);

    CaptureReference sourceSnapshot = sourceCaptureStore.put(interpreted.rawSource());
    CaptureReference textSnapshot = interpreted.normalizedSource() == null || interpreted.normalizedSource().isBlank()
        ? null
        : normalizedCaptureStore.put(interpreted.normalizedSource().getBytes(StandardCharsets.UTF_8));

    Capture capture = mergeCapture(
        withMetadata,
        fetched.metadata().effectiveUrl(),
        detected,
        new MediaMetadata(
            fetched.metadata().statusCode(),
            fetched.metadata().redirectHopCount(),
            fetched.metadata().setCookieCount(),
            fetched.metadata().contentLength() == null
                ? (long) interpreted.rawSource().length
                : fetched.metadata().contentLength()),
        sourceSnapshot,
        textSnapshot);
    ContentDocument document = persist(withMetadata, capture, sourceSnapshot);

    AcquiredContent hydrated = withMetadata
        .withContentDocument(document)
        .withHydratedSource(interpreted.rawSource());
    if (interpreted.normalizedSource() != null && !interpreted.normalizedSource().isBlank()) {
      hydrated = hydrated.withHydratedText(interpreted.normalizedSource(), true);
    }
    return hydrated;
  }

  public AcquiredContent ensureText(@NonNull AcquiredContent current) {
    AcquiredContent withMetadata = current.hasMetadata()
        ? current
        : ensureMetadata(current);
    if (withMetadata.hasCompleteText()) {
      return withMetadata;
    }
    if (withMetadata.hasTextSnapshot()) {
      String text = new String(normalizedCaptureStore.get(withMetadata.capture().interpretedCapture()),
          StandardCharsets.UTF_8);
      return withMetadata.withHydratedText(text, true);
    }

    AcquiredContent withContent = ensureContent(withMetadata);
    if (withContent.hasCompleteText()) {
      return withContent;
    }
    if (!withContent.hasHydratedSource()) {
      return withContent;
    }

    DetectedContent detected = new DetectedContent(
        withContent.capture().mediaType(),
        withContent.capture().mediaEncoding());
    InterpretedContent interpreted = interpret(withContent.hydratedSource(), detected);
    if (interpreted.normalizedSource() == null || interpreted.normalizedSource().isBlank()) {
      return withContent;
    }

    CaptureReference textSnapshot = normalizedCaptureStore
        .put(interpreted.normalizedSource().getBytes(StandardCharsets.UTF_8));
    Capture capture = new Capture(
        withContent.capture().targetId(),
        withContent.capture().canonicalUrl(),
        withContent.capture().mediaType(),
        withContent.capture().mediaEncoding(),
        withContent.capture().mediaMetadata(),
        withContent.capture().sourceCapture(),
        textSnapshot);
    ContentDocument document = persist(withContent, capture, withContent.capture().sourceCapture());
    return withContent.withContentDocument(document)
        .withHydratedText(interpreted.normalizedSource(), true);
  }

  public static DetectedContent detect(String contentTypeHeader) {
    return new DetectedContent(
        resolveMediaType(contentTypeHeader),
        resolveEncoding(contentTypeHeader));
  }

  private static MediaType resolveMediaType(String header) {
    if (header == null || header.isBlank()) {
      return MediaType.OTHER;
    }
    String normalized = header.split(";")[0].trim().toLowerCase(Locale.ROOT);
    if (normalized.contains("html")) {
      return MediaType.TEXT_HTML;
    }
    return switch (normalized) {
      case "application/pdf" -> MediaType.APPLICATION_PDF;
      case "text/plain" -> MediaType.TEXT_PLAIN;
      case "text/markdown" -> MediaType.TEXT_MARKDOWN;
      case "application/xml" -> MediaType.APPLICATION_XML;
      case "text/xml" -> MediaType.TEXT_XML;
      case "application/json" -> MediaType.APPLICATION_JSON;
      case "text/json" -> MediaType.TEXT_JSON;
      case "application/octet-stream" -> MediaType.APPLICATION_OCTET_STREAM;
      default -> MediaType.OTHER;
    };
  }

  private static MediaEncoding resolveEncoding(String header) {
    if (header == null || header.isBlank()) {
      return MediaEncoding.UTF_8;
    }
    String[] parts = header.split(";");
    for (String part : parts) {
      String normalized = part.trim().toLowerCase(Locale.ROOT);
      if (!normalized.startsWith("charset=")) {
        continue;
      }
      String value = normalized.substring("charset=".length()).replace("\"", "");
      return switch (value) {
        case "utf-8" -> MediaEncoding.UTF_8;
        case "utf-16" -> MediaEncoding.UTF_16;
        case "utf-16le" -> MediaEncoding.UTF_16LE;
        case "utf-16be" -> MediaEncoding.UTF_16BE;
        case "us-ascii" -> MediaEncoding.US_ASCII;
        case "iso-8859-1" -> MediaEncoding.ISO_8859_1;
        case "windows-1252" -> MediaEncoding.WINDOWS_1252;
        default -> MediaEncoding.OTHER;
      };
    }
    return MediaEncoding.UTF_8;
  }

  private InterpretedContent interpret(byte[] source, @NonNull DetectedContent detected) {
    for (ContentInterpreter interpreter : interpreters) {
      if (interpreter.supports(detected)) {
        return interpreter.interpret(source, detected);
      }
    }
    return new InterpretedContent(source, null, null);
  }

  private ContentDocument persist(
      AcquiredContent current,
      Capture capture,
      CaptureReference sourceSnapshot) {
    if (current.requestHeader() == null) {
      throw new IllegalStateException("Content acquisition requires a request header");
    }
    UUID reviewId = current.reviewId();
    if (reviewId == null) {
      throw new IllegalStateException("Content acquisition requires a review identifier");
    }
    ContentDocument document = new ContentDocument(
        new DocumentHeader(
            ContentDocument.SCHEMA_VERSION,
            capture.targetId(),
            reviewId,
            current.requestHeader().emittedBy(),
            Instant.now()),
        current.requestHeader(),
        capture,
        sourceSnapshot == null ? null : sourceSnapshot.contentHash());
    contentStore.put(document);
    return document;
  }

  private static Capture mergeCapture(
      AcquiredContent current,
      URL effectiveUrl,
      DetectedContent detected,
      MediaMetadata metadata,
      CaptureReference sourceSnapshot,
      CaptureReference textSnapshot) {
    Capture existing = current.capture();
    URL canonicalUrl = effectiveUrl != null
        ? effectiveUrl
        : existing == null ? current.targetUrl() : existing.canonicalUrl();
    return new Capture(
        targetId(current, existing),
        canonicalUrl,
        detected.mediaType(),
        detected.mediaEncoding(),
        mergeMetadata(existing == null ? null : existing.mediaMetadata(), metadata),
        sourceSnapshot != null ? sourceSnapshot : existing == null ? null : existing.sourceCapture(),
        textSnapshot != null ? textSnapshot : existing == null ? null : existing.interpretedCapture());
  }

  private static Capture withContentLength(Capture capture, Long contentLength) {
    return new Capture(
        capture.targetId(),
        capture.canonicalUrl(),
        capture.mediaType(),
        capture.mediaEncoding(),
        mergeMetadata(capture.mediaMetadata(), new MediaMetadata(null, null, null, contentLength)),
        capture.sourceCapture(),
        capture.interpretedCapture());
  }

  private static MediaMetadata mergeMetadata(MediaMetadata current, MediaMetadata next) {
    if (current == null) {
      return next;
    }
    if (next == null) {
      return current;
    }
    return new MediaMetadata(
        next.statusCode() != null ? next.statusCode() : current.statusCode(),
        next.redirectCount() != null ? next.redirectCount() : current.redirectCount(),
        next.cookieCount() != null ? next.cookieCount() : current.cookieCount(),
        next.contentLength() != null ? next.contentLength() : current.contentLength());
  }

  private static UUID targetId(AcquiredContent current, Capture existing) {
    UUID targetId = current.targetId();
    if (targetId != null) {
      return targetId;
    }
    if (existing != null) {
      return existing.targetId();
    }
    throw new IllegalStateException("Content acquisition requires a target identifier");
  }

  private static URL targetUrl(AcquiredContent current) {
    URL targetUrl = current.targetUrl();
    if (targetUrl == null) {
      throw new IllegalStateException("Content acquisition requires a target URL");
    }
    return targetUrl;
  }
}
