package com.github.jelatinone.infra.aws.store.serial;

import java.util.Map;
import java.util.function.Function;

import com.github.jelatinone.infra.JacksonMapper;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonDynamoSerializer<Value, Key> implements DynamoSerializer<Value, Key> {
  Class<Value> type;

  Function<Key, String> keyEncoder;

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
  public Map<String, AttributeValue> encodeItem(Key key, Value value) throws Exception {
    return Map.of(
        "id", AttributeValue.fromS(keyEncoder.apply(key)),
        "payload", AttributeValue.fromS(JacksonMapper.mapper.writeValueAsString(value)));
  }

  @Override
  public Map<String, AttributeValue> encodeKey(Key key) {
    return Map.of("id", AttributeValue.fromS(keyEncoder.apply(key)));
  }
}
