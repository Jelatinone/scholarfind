package com.github.jelatinone.utility;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;

public final class Canonical {

  private Canonical() {
  }

  public static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  public static URL canonicalizeURL(String value) {
    try {
      return canonicalizeURL(URI.create(value).toURL());
    } catch (IllegalArgumentException | MalformedURLException exception) {
      throw new IllegalArgumentException("Unable to canonicalize malformed URL", exception);
    }
  }

  public static URL canonicalizeURL(@NonNull URL value) {
    try {
      URI uri = value.toURI();
      String scheme = Optional.ofNullable(uri.getScheme()).map(String::toLowerCase).orElse(null);
      String host = Optional.ofNullable(uri.getHost()).map(String::toLowerCase).orElse(null);
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

  public static String normalizePath(String path) {
    String normalized = path == null || path.isBlank() ? "/" : path.replaceAll("/{2,}", "/");
    if (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  public static String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    return query;
  }

  public static URI toURI(URL url) {
    try {
      return url.toURI();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to resolve target URI", exception);
    }
  }

  public static URL toURL(URI uri) {
    try {
      return uri.toURL();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to resolve target URL", exception);
    }
  }

  public static UUID stableUUID(String canonical) {
    return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
  }

  public static boolean isInformational(int statusCode) {
    return statusCode >= 100 && statusCode < 200;
  }

  public static boolean isSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  public static boolean isRedirect(int statusCode) {
    return statusCode >= 300 && statusCode < 400;
  }

  public static boolean isClientError(int statusCode) {
    return statusCode >= 400 && statusCode < 500;
  }

  public static boolean isServerError(int statusCode) {
    return statusCode >= 500 && statusCode < 600;
  }

  public static boolean isError(int statusCode) {
    return isClientError(statusCode) || isServerError(statusCode);
  }
}
