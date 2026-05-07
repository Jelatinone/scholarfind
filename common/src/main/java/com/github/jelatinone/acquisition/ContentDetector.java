package com.github.jelatinone.acquisition;

import java.util.Locale;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

public final class ContentDetector {

  public DetectedContent detect(String contentTypeHeader) {
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
}
