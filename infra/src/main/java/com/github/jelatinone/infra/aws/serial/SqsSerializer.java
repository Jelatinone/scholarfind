package com.github.jelatinone.infra.aws.serial;

import java.util.Map;

public interface SqsSerializer<Value> {
  Value decode(String body, Map<String, String> attributes) throws Exception;

  String encodeBody(Value value) throws Exception;

  Map<String, String> encodeAttributes(Value value) throws Exception;
}
