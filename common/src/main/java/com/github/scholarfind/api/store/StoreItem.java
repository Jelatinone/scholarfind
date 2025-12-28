package com.github.scholarfind.api.store;

import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public record StoreItem(
		UUID key,
		Map<String, AttributeValue> body) {

	public <T> T deserialize(ItemDeserializer<T> deserializer) {
		T envelope = null;
		try {
			envelope = deserializer.decode(body());
		} catch (Exception exception) {
			// TODO: Handling
		}
		return envelope;
	}
}
