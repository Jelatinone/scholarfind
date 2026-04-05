package com.github.jelatinone.infra.aws.jackson;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JavaType;
import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.serial.S3Serializer;

public final class JacksonS3Serializer<Value, Key> implements S3Serializer<Value, Key> {
  private final JavaType type;
  private final BiFunction<Value, byte[], Key> valueKeyExtractor;
  private final Function<Key, String> keyEncoder;
  private final String contentType;
  private final String contentEncoding;
  private final Function<Value, Map<String, String>> metadataFactory;

  public JacksonS3Serializer(
      Class<Value> type,
      Function<Value, Key> valueKeyExtractor,
      Function<Key, String> keyEncoder) {
    this(
        JacksonMapper.mapper.getTypeFactory().constructType(type),
        (value, encoded) -> valueKeyExtractor.apply(value),
        keyEncoder,
        "application/json",
        "utf-8",
        value -> Map.of());
  }

  public JacksonS3Serializer(
      JavaType type,
      BiFunction<Value, byte[], Key> valueKeyExtractor,
      Function<Key, String> keyEncoder,
      String contentType,
      String contentEncoding,
      Function<Value, Map<String, String>> metadataFactory) {
    this.type = type;
    this.valueKeyExtractor = valueKeyExtractor;
    this.keyEncoder = keyEncoder;
    this.contentType = contentType;
    this.contentEncoding = contentEncoding;
    this.metadataFactory = metadataFactory;
  }

  @Override
  public Value decode(StoredValue<Key> item) throws Exception {
    if (item == null || item.body().length == 0) {
      return null;
    }
    return JacksonMapper.mapper.readValue(item.body(), type);
  }

  @Override
  public EncodedValue<Key> encode(Value value) throws Exception {
    byte[] bytes = JacksonMapper.mapper.writeValueAsBytes(value);
    Key key = valueKeyExtractor.apply(value, bytes);
    return new EncodedValue<>(
        key,
        bytes,
        contentType,
        contentEncoding,
        metadataFactory.apply(value));
  }

  @Override
  public String encodeKey(Key key) {
    return keyEncoder.apply(key);
  }
}
