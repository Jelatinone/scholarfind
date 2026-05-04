package com.github.jelatinone.infra.aws.store;

import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.infra.aws.store.serial.DynamoSerializer;

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
public class DynamoStore<Value, Key> implements Store<Value, Key> {
  DynamoDbClient client;
  String table;

  DynamoSerializer<Value, Key> serializer;

  static Logger _logger = LoggerFactory.getLogger(DynamoStore.class);

  @Override
  public Key put(Value body) {
    try {
      PutItemResponse response = client.putItem(builder -> builder
          .tableName(table)
          .item(encode(body))
          .build());
      logResponse("Put item", response.sdkHttpResponse());
      return serializer.deriveKey(body);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode store item", exception);
    }
  }

  public TransactWriteItem transactPut(Value body) {
    return transactPut(body, (builder) -> {
      // Do nothing ;)
    });
  }

  public TransactWriteItem transactPut(
      Value body,
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
  public Value get(Key key) {
    try {
      GetItemResponse response = client.getItem(builder -> builder
          .tableName(table)
          .key(serializer.encodeKey(key))
          .build());
      logResponse("Get item", response.sdkHttpResponse());
      return serializer.decodeItem(response.item());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to decode store item", exception);
    }
  }

  public TransactGetItem transactGet(Key key) {
    return transactGet(key, (builder) -> {
      // Do nothing ;)
    });
  }

  public TransactGetItem transactGet(Key key, Consumer<Get.Builder> mutator) {
    try {
      var item = serializer.encodeKey(key);
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
  public void delete(Key key) {
    try {
      DeleteItemResponse response = client.deleteItem(builder -> builder
          .tableName(table)
          .key(serializer.encodeKey(key))
          .build());
      logResponse("Delete item", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to delete store item", exception);
    }
  }

  public TransactWriteItem transactDelete(Key key) {
    return transactDelete(key, (builder) -> {
      // Do nothing ;)
    });
  }

  public TransactWriteItem transactDelete(Key key, Consumer<Delete.Builder> mutator) {
    try {
      var item = serializer.encodeKey(key);
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

  public Map<String, AttributeValue> encode(Value body) {
    try {
      return serializer.encodeItem(body);
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
