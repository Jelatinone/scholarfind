package com.github.jelatinone.infra.aws.serial;

import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public interface DynamoSerializer<Value, Key> {
  Map<String, AttributeValue> encodeItem(Value value) throws Exception;

  Value decodeItem(Map<String, AttributeValue> item) throws Exception;

  Map<String, AttributeValue> encodeKey(Key key);

  Key deriveKey(Value value);
}
