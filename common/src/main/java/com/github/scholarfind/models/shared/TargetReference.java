package com.github.scholarfind.models.shared;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

import com.github.scholarfind.utility.Canonical;

public record TargetReference(
    UUID targetId,
    URL normalizedUrl,
    UUID parentTargetId,
    int depth,
    Instant discoveredAt) {

  public static TargetReference canonical(
      URL url,
      UUID parentTargetId,
      int depth,
      Instant discoveredAt) {
    return Canonical.target(url, parentTargetId, depth, discoveredAt);
  }

  public static TargetReference canonical(
      String url,
      UUID parentTargetId,
      int depth,
      Instant discoveredAt) {
    return Canonical.target(url, parentTargetId, depth, discoveredAt);
  }
}
