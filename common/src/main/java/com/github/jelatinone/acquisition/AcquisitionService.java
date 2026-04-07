package com.github.jelatinone.acquisition;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.models.content.Capture;
import com.github.jelatinone.models.content.CharacterEncoding;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.content.ContentKind;
import com.github.jelatinone.models.content.ContentMediaType;
import com.github.jelatinone.models.shared.DocumentHeader;
import com.github.jelatinone.models.shared.FetchReference;

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
  Store<byte[], FetchReference> sourceCaptureStore;
  Store<byte[], FetchReference> normalizedCaptureStore;

  public AcquiredContent ensureMetadata(@NonNull AcquiredContent current) {
    if (current.hasMetadata()) {
      return current;
    }

    FetchedMetadata fetched = metadataFetcher.fetch(targetUrl(current));
    DetectedContent detected = detect(fetched.contentTypeHeader());
    Capture capture = mergeCapture(
        current.capture(),
        fetched.effectiveUrl(),
        fetched.statusCode(),
        fetched.redirectHopCount(),
        fetched.setCookieCount(),
        detected,
        fetched.contentLength(),
        current.capture() == null ? null : current.capture().sourceSnapshot(),
        current.capture() == null ? null : current.capture().textSnapshot(),
        current.capture() == null ? null : current.capture().previewText());
    return current.withContentDocument(persist(current, capture));
  }

  public AcquiredContent ensureContent(@NonNull AcquiredContent current) {
    AcquiredContent withMetadata = current.hasMetadata()
        ? current
        : ensureMetadata(current);
    if (withMetadata.hasHydratedSource()) {
      return withMetadata;
    }
    if (withMetadata.hasSourceSnapshot()) {
      byte[] source = sourceCaptureStore.get(withMetadata.capture().sourceSnapshot());
      Capture capture = withMetadata.capture().contentLength() == null
          ? new Capture(
              withMetadata.capture().effectiveUrl(),
              withMetadata.capture().statusCode(),
              withMetadata.capture().redirectHopCount(),
              withMetadata.capture().setCookieCount(),
              withMetadata.capture().sourceSnapshot(),
              withMetadata.capture().textSnapshot(),
              withMetadata.capture().contentKind(),
              withMetadata.capture().mediaType(),
              withMetadata.capture().characterEncoding(),
              (long) source.length,
              withMetadata.capture().previewText())
          : withMetadata.capture();
      AcquiredContent hydrated = withMetadata.withHydratedSource(source);
      if (capture != withMetadata.capture()) {
        hydrated = hydrated.withContentDocument(persist(hydrated, capture));
      }
      return hydrated;
    }

    FetchedBody fetched = bodyFetcher.fetch(targetUrl(withMetadata));
    DetectedContent detected = detect(fetched.metadata().contentTypeHeader());
    InterpretedContent interpreted = interpret(fetched.body(), detected);

    FetchReference sourceSnapshot = sourceCaptureStore.put(interpreted.rawSource());
    FetchReference textSnapshot = interpreted.normalizedSource() == null || interpreted.normalizedSource().isBlank()
        ? null
        : normalizedCaptureStore.put(interpreted.normalizedSource().getBytes(StandardCharsets.UTF_8));

    Capture capture = mergeCapture(
        withMetadata.capture(),
        fetched.metadata().effectiveUrl(),
        fetched.metadata().statusCode(),
        fetched.metadata().redirectHopCount(),
        fetched.metadata().setCookieCount(),
        detected,
        fetched.metadata().contentLength() == null ? (long) interpreted.rawSource().length
            : fetched.metadata().contentLength(),
        sourceSnapshot,
        textSnapshot,
        interpreted.normalizedPreview());
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
      String text = new String(normalizedCaptureStore.get(withMetadata.capture().textSnapshot()),
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
        withContent.capture().contentKind(),
        withContent.capture().mediaType(),
        withContent.capture().characterEncoding());
    InterpretedContent interpreted = interpret(withContent.hydratedSource(), detected);
    if (interpreted.normalizedSource() == null || interpreted.normalizedSource().isBlank()) {
      return withContent;
    }

    FetchReference textSnapshot = normalizedCaptureStore
        .put(interpreted.normalizedSource().getBytes(StandardCharsets.UTF_8));
    Capture capture = new Capture(
        withContent.capture().effectiveUrl(),
        withContent.capture().statusCode(),
        withContent.capture().redirectHopCount(),
        withContent.capture().setCookieCount(),
        withContent.capture().sourceSnapshot(),
        textSnapshot,
        withContent.capture().contentKind(),
        withContent.capture().mediaType(),
        withContent.capture().characterEncoding(),
        withContent.capture().contentLength(),
        interpreted.normalizedPreview());
    ContentDocument document = persist(withContent, capture, withContent.capture().sourceSnapshot());
    return withContent.withContentDocument(document)
        .withHydratedText(interpreted.normalizedSource(), true);
  }

  public static DetectedContent detect(String contentTypeHeader) {
    ContentMediaType mediaType = resolveMediaType(contentTypeHeader);
    return new DetectedContent(
        resolveContentKind(mediaType),
        mediaType,
        resolveEncoding(contentTypeHeader));
  }

  private static ContentMediaType resolveMediaType(String header) {
    if (header == null || header.isBlank()) {
      return ContentMediaType.OTHER;
    }
    String normalized = header.split(";")[0].trim().toLowerCase(Locale.ROOT);
    if (normalized.contains("html")) {
      return ContentMediaType.TEXT_HTML;
    }
    return switch (normalized) {
      case "application/pdf" -> ContentMediaType.APPLICATION_PDF;
      case "text/plain" -> ContentMediaType.TEXT_PLAIN;
      case "text/markdown" -> ContentMediaType.TEXT_MARKDOWN;
      case "application/xml" -> ContentMediaType.APPLICATION_XML;
      case "text/xml" -> ContentMediaType.TEXT_XML;
      case "application/json" -> ContentMediaType.APPLICATION_JSON;
      case "text/json" -> ContentMediaType.TEXT_JSON;
      case "application/octet-stream" -> ContentMediaType.APPLICATION_OCTET_STREAM;
      default -> ContentMediaType.OTHER;
    };
  }

  private static CharacterEncoding resolveEncoding(String header) {
    if (header == null || header.isBlank()) {
      return CharacterEncoding.UTF_8;
    }
    String[] parts = header.split(";");
    for (String part : parts) {
      String normalized = part.trim().toLowerCase(Locale.ROOT);
      if (!normalized.startsWith("charset=")) {
        continue;
      }
      String value = normalized.substring("charset=".length()).replace("\"", "");
      return switch (value) {
        case "utf-8" -> CharacterEncoding.UTF_8;
        case "utf-16" -> CharacterEncoding.UTF_16;
        case "utf-16le" -> CharacterEncoding.UTF_16LE;
        case "utf-16be" -> CharacterEncoding.UTF_16BE;
        case "us-ascii" -> CharacterEncoding.US_ASCII;
        case "iso-8859-1" -> CharacterEncoding.ISO_8859_1;
        case "windows-1252" -> CharacterEncoding.WINDOWS_1252;
        default -> CharacterEncoding.OTHER;
      };
    }
    return CharacterEncoding.UTF_8;
  }

  private static ContentKind resolveContentKind(ContentMediaType mediaType) {
    return switch (mediaType == null ? ContentMediaType.OTHER : mediaType) {
      case TEXT_HTML -> ContentKind.HTML;
      case APPLICATION_PDF -> ContentKind.PDF;
      case TEXT_PLAIN -> ContentKind.PLAIN_TEXT;
      case TEXT_MARKDOWN -> ContentKind.MARKDOWN;
      case APPLICATION_XML, TEXT_XML -> ContentKind.XML;
      case APPLICATION_JSON, TEXT_JSON -> ContentKind.JSON;
      default -> ContentKind.OTHER;
    };
  }

  private InterpretedContent interpret(@NonNull byte[] source, @NonNull DetectedContent detected) {
    for (ContentInterpreter interpreter : interpreters) {
      if (interpreter.supports(detected)) {
        return interpreter.interpret(source, detected);
      }
    }
    return new InterpretedContent(source, null, null);
  }

  private ContentDocument persist(AcquiredContent current, Capture capture) {
    return persist(current, capture, capture == null ? null : capture.sourceSnapshot());
  }

  private ContentDocument persist(
      AcquiredContent current,
      Capture capture,
      FetchReference sourceSnapshot) {
    ContentDocument document = new ContentDocument(
        new DocumentHeader(
            ContentDocument.SCHEMA_VERSION,
            UUID.randomUUID(),
            current.requestHeader().requestId(),
            current.target().targetId(),
            Instant.now()),
        current.requestHeader(),
        current.target(),
        capture,
        sourceSnapshot == null ? null : sourceSnapshot.contentHash());
    contentStore.put(document);
    return document;
  }

  private static Capture mergeCapture(
      Capture current,
      URL effectiveUrl,
      Integer statusCode,
      Integer redirectHopCount,
      Integer setCookieCount,
      DetectedContent detected,
      Long contentLength,
      FetchReference sourceSnapshot,
      FetchReference textSnapshot,
      String previewText) {
    return new Capture(
        effectiveUrl,
        statusCode,
        redirectHopCount,
        setCookieCount,
        sourceSnapshot != null ? sourceSnapshot : current == null ? null : current.sourceSnapshot(),
        textSnapshot != null ? textSnapshot : current == null ? null : current.textSnapshot(),
        detected.contentKind(),
        detected.mediaType(),
        detected.characterEncoding(),
        contentLength != null ? contentLength : current == null ? null : current.contentLength(),
        previewText != null ? previewText : current == null ? null : current.previewText());
  }

  private static URL targetUrl(AcquiredContent current) {
    URL targetUrl = current.targetUrl();
    if (targetUrl == null) {
      throw new IllegalStateException("Content acquisition requires a target URL");
    }
    return targetUrl;
  }
}
