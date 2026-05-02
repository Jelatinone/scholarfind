package com.github.jelatinone.infra.aws.serial.jackson;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JavaType;
import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.serial.S3Serializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonS3Serializer<Value, Key> implements S3Serializer<Value, Key> {
  JavaType type;

  BiFunction<Value, byte[], Key> keyExtractor;
  Function<Key, String> keyEncoder;
  Function<Value, Map<String, String>> metadataEncoder;

  String contentType;
  String contentEncoding;

  public JacksonS3Serializer(
      Class<Value> type,
      Function<Value, Key> keyExtractor,
      Function<Key, String> keyEncoder) {
    this(
        JacksonMapper.mapper.getTypeFactory().constructType(type),
        (value, encoded) -> keyExtractor.apply(value),
        keyEncoder, value -> Map.of(),
        "application/json",
        "utf-8");
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
    Key key = keyExtractor.apply(value, bytes);
    return new EncodedValue<>(
        key,
        bytes,
        contentType,
        contentEncoding,
        metadataEncoder.apply(value));
  }

  @Override
  public String encodeKey(Key key) {
    return keyEncoder.apply(key);
  }
}
