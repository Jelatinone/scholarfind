package com.github.scholarfind.aws;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.slf4j.event.Level;

import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.api.store.StoreItem;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DynamoStore implements Store<Map<String, AttributeValue>, UUID> {

	DynamoDbClient _client;
	String _table;
	BiConsumer<String, Level> _logger;

	public void put(String table, Map<String, AttributeValue> body) {
		PutItemResponse putItemResponse = _client.putItem(builder -> builder.item(body)
				.tableName(_table)
				.build());

		SdkHttpResponse requestSdkResponse = putItemResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Put item to table completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Put item to table failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}
	}

	@Override
	public void put(Map<String, AttributeValue> body) {
		put(_table, body);
	}

	public StoreItem get(String table, UUID key) {
		GetItemResponse getItemResponse = _client.getItem(builder -> builder
				.tableName(table)
				.key(Map.of(
						"id", AttributeValue.fromS(key.toString())))
				.build());

		SdkHttpResponse requestSdkResponse = getItemResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Get item from table completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Get item from table failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}

		StoreItem item = new StoreItem(key, getItemResponse.item());
		return item;
	}

	@Override
	public StoreItem get(UUID key) {
		return get(_table, key);
	}

	public void delete(String table, UUID key) {
		DeleteItemResponse deleteItemResponse = _client.deleteItem(builder -> builder
				.tableName(table)
				.key(Map.of(
						"id", AttributeValue.fromS(key.toString())))
				.build());

		SdkHttpResponse requestSdkResponse = deleteItemResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Delete item from table completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Delete item from table failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}
	}

	@Override
	public void delete(UUID key) {
		delete(_table, key);
	}

	@Override
	public void close() {
		_client.close();
	}
}
