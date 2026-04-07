package com.github.jelatinone.infra.repository;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import com.github.jelatinone.infra.aws.S3Store;
import com.github.jelatinone.infra.aws.serial.S3Serializer;
import com.github.jelatinone.models.shared.FetchReference;

import software.amazon.awssdk.services.s3.S3Client;

public final class CaptureDocumentStore extends S3Store<byte[], FetchReference> {
  public CaptureDocumentStore(S3Client client, String bucket, String prefix) {
    super(client, bucket, new CaptureS3Serializer(prefix));
  }

  private static final class CaptureS3Serializer implements S3Serializer<byte[], FetchReference> {
    private final String _prefix;

    private CaptureS3Serializer(String prefix) {
      _prefix = normalize(prefix);
    }

    @Override
    public EncodedValue<FetchReference> encode(byte[] value) throws Exception {
      byte[] bytes = value == null ? new byte[0] : value;
      String snapshotId = _prefix + UUID.randomUUID();
      String contentHash = hash(bytes);
      FetchReference reference = new FetchReference(snapshotId, contentHash, Instant.now());
      return new EncodedValue<>(
          reference,
          bytes,
          "application/octet-stream",
          null,
          Map.of(
              "contentHash", contentHash,
              "fetchedAt", reference.fetchedAt().toString()));
    }

    @Override
    public byte[] decode(StoredValue<FetchReference> value) throws Exception {
      return value == null ? null : value.body();
    }

    @Override
    public String encodeKey(FetchReference key) {
      return key.snapshotId();
    }
  }

  private static String normalize(String prefix) {
    if (prefix == null || prefix.isBlank()) {
      return "";
    }
    return prefix.endsWith("/") ? prefix : prefix + "/";
  }

  private static String hash(byte[] content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(content));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to hash captured content", exception);
    }
  }
}
