package com.github.jelatinone.infra.aws.jackson;

import java.util.Map;
import java.util.function.Function;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.serial.DynamoSerializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public final class JacksonDynamoSerializer<Value, Key> implements DynamoSerializer<Value, Key> {
  private final Class<Value> type;
  private final Function<Key, String> keyEncoder;
  private final Function<Value, Key> valueKeyExtractor;

  public JacksonDynamoSerializer(Class<Value> type, Function<Key, String> keyEncoder,
      Function<Value, Key> valueKeyExtractor) {
    this.type = type;
    this.keyEncoder = keyEncoder;
    this.valueKeyExtractor = valueKeyExtractor;
  }

  @Override
  public Value decodeItem(Map<String, AttributeValue> item) throws Exception {
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
  public Map<String, AttributeValue> encodeItem(Value value) throws Exception {
    Key key = valueKeyExtractor.apply(value);
    return Map.of(
        "id", AttributeValue.fromS(keyEncoder.apply(key)),
        "payload", AttributeValue.fromS(JacksonMapper.mapper.writeValueAsString(value)));
  }

  @Override
  public Key deriveKey(Value value) {
    return valueKeyExtractor.apply(value);
  }

  @Override
  public Map<String, AttributeValue> encodeKey(Key key) {
    return Map.of("id", AttributeValue.fromS(keyEncoder.apply(key)));
  }
}
