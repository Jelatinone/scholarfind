package com.github.jelatinone.infra.aws.queue.serial;

import com.fasterxml.jackson.databind.JavaType;
import com.github.jelatinone.infra.JacksonMapper;

import java.io.IOException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonSQSSerializer<Value> implements SQSSerializer<Value> {
  JavaType type;

  @Override
  public Value decode(String body, Map<String, String> attributes) throws IOException {
    return JacksonMapper.mapper.readValue(body, type);
  }

  @Override
  public String encodeBody(Value value) throws IOException {
    return JacksonMapper.mapper.writeValueAsString(value);
  }

  @Override
  public Map<String, String> encodeAttributes(Value value) {
    return Map.of();
  }
}
