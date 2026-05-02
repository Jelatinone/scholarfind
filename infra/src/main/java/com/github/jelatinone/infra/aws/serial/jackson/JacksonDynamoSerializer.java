package com.github.jelatinone.infra.aws.serial.jackson;

import java.util.Map;
import java.util.function.Function;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.serial.DynamoSerializer;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonDynamoSerializer<Value, Key> implements DynamoSerializer<Value, Key> {
  Class<Value> type;

  Function<Key, String> keyEncoder;
  Function<Value, Key> keyExtractor;

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
    Key key = keyExtractor.apply(value);
    return Map.of(
        "id", AttributeValue.fromS(keyEncoder.apply(key)),
        "payload", AttributeValue.fromS(JacksonMapper.mapper.writeValueAsString(value)));
  }

  @Override
  public Key deriveKey(Value value) {
    return keyExtractor.apply(value);
  }

  @Override
  public Map<String, AttributeValue> encodeKey(Key key) {
    return Map.of("id", AttributeValue.fromS(keyEncoder.apply(key)));
  }
}
