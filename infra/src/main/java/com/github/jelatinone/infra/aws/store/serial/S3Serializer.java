package com.github.jelatinone.infra.aws.store.serial;

import java.util.Map;

public interface S3Serializer<Value, Key> {
  EncodedValue<Key> encode(Value value) throws Exception;

  Value decode(StoredValue<Key> value) throws Exception;

  String encodeKey(Key key);

  record EncodedValue<Key>(
      Key key,
      byte[] body,
      String contentType,
      String contentEncoding,
      Map<String, String> metadata) {
    public EncodedValue {
      body = body == null ? new byte[0] : body;
      metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
  }

  record StoredValue<Key>(
      Key key,
      byte[] body,
      String contentType,
      String contentEncoding,
      Map<String, String> metadata) {
    public StoredValue {
      body = body == null ? new byte[0] : body;
      metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
  }
}
