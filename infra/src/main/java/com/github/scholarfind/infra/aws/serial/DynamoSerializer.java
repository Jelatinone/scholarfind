package com.github.scholarfind.infra.aws.serial;

import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public interface DynamoSerializer<T, K> {
  T decode(Map<String, AttributeValue> item) throws Exception;

  Map<String, AttributeValue> encode(T value) throws Exception;

  Map<String, AttributeValue> key(K key);
}
