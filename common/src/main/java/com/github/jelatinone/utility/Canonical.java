package com.github.jelatinone.utility;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import com.github.jelatinone.models.shared.TargetReference;

public final class Canonical {

  private Canonical() {
  }

  public static URL canonicalizeURL(String value) {
    try {
      return canonicalizeURL(URI.create(value).toURL());
    } catch (IllegalArgumentException | MalformedURLException exception) {
      throw new IllegalArgumentException("Unable to canonicalize malformed URL", exception);
    }
  }

  public static URL canonicalizeURL(URL value) {
    if (value == null) {
      throw new IllegalArgumentException("Unable to canonicalize null URL");
    }

    try {
      URI uri = value.toURI();
      String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase();
      String host = uri.getHost() == null ? null : uri.getHost().toLowerCase();
      int port = normalizePort(scheme, uri.getPort());
      String path = normalizePath(uri.getPath());
      String query = normalizeQuery(uri.getRawQuery());

      return new URI(
          scheme,
          uri.getRawUserInfo(),
          host,
          port,
          path,
          query,
          null)
          .toURL();
    } catch (URISyntaxException | MalformedURLException exception) {
      throw new IllegalArgumentException("Unable to canonicalize URL", exception);
    }
  }

  public static UUID generateTargetUUID(URL value) {
    URL canonical = canonicalizeURL(value);
    return UUID.nameUUIDFromBytes(canonical.toExternalForm().getBytes(StandardCharsets.UTF_8));
  }

  public static TargetReference target(
      URL value,
      UUID parentTargetId,
      int depth,
      Instant discoveredAt) {
    URL canonical = canonicalizeURL(value);
    return new TargetReference(
        generateTargetUUID(canonical),
        canonical,
        parentTargetId,
        depth,
        discoveredAt);
  }

  public static TargetReference target(
      String value,
      UUID parentTargetId,
      int depth,
      Instant discoveredAt) {
    return target(canonicalizeURL(value), parentTargetId, depth, discoveredAt);
  }

  private static int normalizePort(String scheme, int port) {
    if (port < 0) {
      return -1;
    }
    if ("http".equalsIgnoreCase(scheme) && port == 80) {
      return -1;
    }
    if ("https".equalsIgnoreCase(scheme) && port == 443) {
      return -1;
    }
    return port;
  }

  private static String normalizePath(String path) {
    String normalized = path == null || path.isBlank() ? "/" : path.replaceAll("/{2,}", "/");
    if (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private static String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    return query;
  }
}
