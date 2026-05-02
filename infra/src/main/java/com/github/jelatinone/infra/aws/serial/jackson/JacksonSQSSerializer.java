package com.github.jelatinone.infra.aws.serial.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.serial.SQSSerializer;

import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonSQSSerializer<Value> implements SQSSerializer<Value> {
  JavaType type;

  @Override
  public Value decode(String body, Map<String, String> attributes) throws Exception {
    return JacksonMapper.mapper.readValue(body, type);
  }

  @Override
  public String encodeBody(Value value) throws Exception {
    return JacksonMapper.mapper.writeValueAsString(value);
  }

  @Override
  public Map<String, String> encodeAttributes(Value value) {
    return Map.of();
  }
}
