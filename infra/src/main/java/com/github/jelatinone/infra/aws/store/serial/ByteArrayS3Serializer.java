package com.github.jelatinone.infra.aws.store.serial;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import com.github.jelatinone.model.content.CaptureReference;

import lombok.NonNull;

public final class ByteArrayS3Serializer implements S3Serializer<byte[], CaptureReference> {
  private static final UUID UNKNOWN_TARGET = new UUID(0L, 0L);

  private final String prefix;
  private final Clock clock;

  public ByteArrayS3Serializer(@NonNull String prefix) {
    this(prefix, Clock.systemUTC());
  }

  public ByteArrayS3Serializer(@NonNull String prefix, @NonNull Clock clock) {
    this.prefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    this.clock = clock;
  }

  @Override
  public byte[] decode(StoredValue<CaptureReference> item) {
    return item == null ? null : item.body();
  }

  @Override
  public EncodedValue<CaptureReference> encode(byte[] value) throws Exception {
    String hash = hash(value);
    CaptureReference reference = new CaptureReference(
        UNKNOWN_TARGET,
        "%s/%s".formatted(prefix, hash),
        hash,
        Instant.now(clock));
    return new EncodedValue<>(
        reference,
        value,
        "application/octet-stream",
        null,
        Map.of("content-hash", hash));
  }

  @Override
  public String encodeKey(CaptureReference key) {
    return key.storeKey();
  }

  private static String hash(byte[] value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value));
  }
}
