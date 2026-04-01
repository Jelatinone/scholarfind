package com.github.jelatinone.infra.aws;

import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.infra.aws.serial.DynamoSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DynamoStore<T, K> implements Store<T, K> {
  DynamoDbClient client;
  String table;
  DynamoSerializer<T, K> serializer;

  static Logger _logger = LoggerFactory.getLogger(DynamoStore.class);

  @Override
  public void put(T body) {
    try {
      PutItemResponse response = client.putItem(builder -> builder
          .tableName(table)
          .item(encode(body))
          .build());
      logResponse("Put item", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode store item", exception);
    }
  }

  public TransactWriteItem transactPut(T body) {
    return transactPut(body, (builder) -> {
      // Do nothing ;)
    });
  }

  public TransactWriteItem transactPut(
      T body,
      Consumer<Put.Builder> mutator) {
    try {
      return TransactWriteItem.builder()
          .put(Put.builder()
              .tableName(table)
              .item(encode(body))
              .applyMutation(mutator)
              .build())
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to transact store get item", exception);
    }
  }

  @Override
  public T get(K key) {
    try {
      GetItemResponse response = client.getItem(builder -> builder
          .tableName(table)
          .key(serializer.key(key))
          .build());
      logResponse("Get item", response.sdkHttpResponse());
      return serializer.decode(response.item());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to decode store item", exception);
    }
  }

  public TransactGetItem transactGet(K key) {
    return transactGet(key, (builder) -> {
      // Do nothing ;)
    });
  }

  public TransactGetItem transactGet(K key, Consumer<Get.Builder> mutator) {
    try {
      var item = serializer.key(key);
      TransactGetItem transact = TransactGetItem.builder()
          .get(Get.builder()
              .tableName(table)
              .key(item)
              .applyMutation(mutator)
              .build())
          .build();
      return transact;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to transact get store item", exception);
    }
  }

  @Override
  public void delete(K key) {
    try {
      DeleteItemResponse response = client.deleteItem(builder -> builder
          .tableName(table)
          .key(serializer.key(key))
          .build());
      logResponse("Delete item", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to delete store item", exception);
    }
  }

  public TransactWriteItem transactDelete(K key) {
    return transactDelete(key, (builder) -> {
      // Do nothing ;)
    });
  }

  public TransactWriteItem transactDelete(K key, Consumer<Delete.Builder> mutator) {
    try {
      var item = serializer.key(key);
      TransactWriteItem transact = TransactWriteItem.builder()
          .delete(Delete.builder()
              .tableName(table)
              .key(item)
              .applyMutation(mutator)
              .build())
          .build();
      return transact;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to transact delete store item", exception);
    }
  }

  public Map<String, AttributeValue> encode(T body) {
    try {
      return serializer.encode(body);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode store item", exception);
    }
  }

  private void logResponse(String action, SdkHttpResponse response) {
    if (response == null) {
      return;
    }
    String message = String.format("%s completed : [%d] %s", action, response.statusCode(), response.statusText());
    if (response.isSuccessful()) {
      _logger.info(message);
      return;
    }
    _logger.error(message);
  }

  @Override
  public void close() {
    client.close();
  }
}
