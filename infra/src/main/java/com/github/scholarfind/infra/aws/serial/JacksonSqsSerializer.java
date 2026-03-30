package com.github.scholarfind.infra.aws.serial;

import com.fasterxml.jackson.databind.JavaType;
import com.github.scholarfind.infra.JacksonMapper;

import java.util.Map;

public final class JacksonSqsSerializer<T> implements SqsSerializer<T> {
  private final JavaType type;

  public JacksonSqsSerializer(Class<T> type) {
    this.type = JacksonMapper.mapper.getTypeFactory().constructType(type);
  }

  public JacksonSqsSerializer(JavaType type) {
    this.type = type;
  }

  @Override
  public T decode(String body, Map<String, String> attributes) throws Exception {
    return JacksonMapper.mapper.readValue(body, type);
  }

  @Override
  public String encodeBody(T value) throws Exception {
    return JacksonMapper.mapper.writeValueAsString(value);
  }

  @Override
  public Map<String, String> encodeAttributes(T value) {
    return Map.of();
  }
}
