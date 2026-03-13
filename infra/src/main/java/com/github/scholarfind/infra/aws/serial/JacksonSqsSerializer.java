package com.github.scholarfind.infra.aws.serial;

import java.util.Map;

import com.github.scholarfind.infra.aws.JacksonMapper;

public final class JacksonSqsSerializer<T> implements SqsSerializer<T> {
  private final Class<T> type;

  public JacksonSqsSerializer(Class<T> type) {
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
