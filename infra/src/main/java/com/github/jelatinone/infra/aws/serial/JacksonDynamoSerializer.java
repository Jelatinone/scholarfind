package com.github.jelatinone.infra.aws.serial;

import java.util.Map;
import java.util.function.Function;

import com.github.jelatinone.infra.JacksonMapper;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public final class JacksonDynamoSerializer<T, K> implements DynamoSerializer<T, K> {
  private final Class<T> type;
  private final Function<K, String> keyEncoder;
  private final Function<T, String> valueKeyExtractor;

  public JacksonDynamoSerializer(Class<T> type, Function<K, String> keyEncoder, Function<T, String> valueKeyExtractor) {
    this.type = type;
    this.keyEncoder = keyEncoder;
    this.valueKeyExtractor = valueKeyExtractor;
  }

  @Override
  public T decode(Map<String, AttributeValue> item) throws Exception {
    if (item == null || item.isEmpty()) {
      return null;
    }
    AttributeValue payload = item.get("payload");
    if (payload == null || payload.s() == null) {
      return null;
    }
    return JacksonMapper.mapper.readValue(payload.s(), type);
  }

  @Override
  public Map<String, AttributeValue> encode(T value) throws Exception {
    return Map.of(
        "id", AttributeValue.fromS(valueKeyExtractor.apply(value)),
        "payload", AttributeValue.fromS(JacksonMapper.mapper.writeValueAsString(value)));
  }

  @Override
  public Map<String, AttributeValue> key(K key) {
    return Map.of("id", AttributeValue.fromS(keyEncoder.apply(key)));
  }
}
