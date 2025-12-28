package com.github.scholarfind.api.store;

import java.util.Map;

import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@FunctionalInterface
public interface ItemDeserializer<T> {
	T decode(@NonNull Map<String, AttributeValue> body) throws Exception;
}
