package com.github.scholarfind.models.ingest;

import java.time.Instant;
import java.util.UUID;

import com.github.scholarfind.models.shared.TargetReference;
import com.github.scholarfind.utility.Canonical;

public record TargetRecord(
    UUID targetId,
    String canonicalUrl,
    String normalizedHost,
    Instant firstSeenAt,
    Instant lastSeenAt,
    Instant lastScheduledAt,
    IngestProvenance lastProvenance,
    Integer lastPriority,
    int minimumDepthObserved,
    int seenCount) {

  public static String key(TargetReference target) {
    return key(target.normalizedUrl().toExternalForm());
  }

  public static String key(String url) {
    return Canonical.canonicalizeURL(url).toExternalForm();
  }

  public static TargetRecord upsert(TargetRecord current, IngestDocument document) {
    TargetReference target = document.target();
    Instant occurredAt = document.documentHeader().createdAt();
    String canonicalUrl = key(target);
    String normalizedHost = target.normalizedUrl() == null ? null : target.normalizedUrl().getHost();

    Instant firstSeenAt = current == null || current.firstSeenAt() == null
        ? occurredAt
        : current.firstSeenAt();
    Instant lastScheduledAt = document.admitted()
        ? occurredAt
        : current == null ? null : current.lastScheduledAt();
    int minimumDepthObserved = current == null
        ? target.depth()
        : Math.min(current.minimumDepthObserved(), target.depth());
    int seenCount = current == null ? 1 : current.seenCount() + 1;

    return new TargetRecord(
        target.targetId(),
        canonicalUrl,
        normalizedHost,
        firstSeenAt,
        occurredAt,
        lastScheduledAt,
        document.provenance(),
        document.priority(),
        minimumDepthObserved,
        seenCount);
  }
}
