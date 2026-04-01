package com.github.jelatinone.infra.aws.serial;

import java.util.Map;

public interface SqsSerializer<T> {
  T decode(String body, Map<String, String> attributes) throws Exception;

  String encodeBody(T value) throws Exception;

  Map<String, String> encodeAttributes(T value) throws Exception;
}
