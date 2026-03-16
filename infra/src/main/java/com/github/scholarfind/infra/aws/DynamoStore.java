package com.github.scholarfind.infra.aws;

import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.infra.aws.serial.DynamoSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DynamoStore<T, K> implements Store<T, K> {
  DynamoDbClient client;
  String table;
  DynamoSerializer<T, K> serializer;
  BiConsumer<String, Level> logger;

  @Override
  public void put(T body) {
    try {
      var item = serializer.encode(body);
      PutItemResponse response = client.putItem(builder -> builder
          .tableName(table)
          .item(item)
          .build());
      log("Put item", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode store item", exception);
    }
  }

  @Override
  public T get(K key) {
    try {
      GetItemResponse response = client.getItem(builder -> builder
          .tableName(table)
          .key(serializer.key(key))
          .build());
      log("Get item", response.sdkHttpResponse());
      return serializer.decode(response.item());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to decode store item", exception);
    }
  }

  @Override
  public void delete(K key) {
    DeleteItemResponse response = client.deleteItem(builder -> builder
        .tableName(table)
        .key(serializer.key(key))
        .build());
    log("Delete item", response.sdkHttpResponse());
  }

  private void log(String action, SdkHttpResponse response) {
    if (response == null) {
      return;
    }
    Level level = response.isSuccessful() ? Level.INFO : Level.ERROR;
    logger.accept(
        String.format("%s completed : [%d] %s", action, response.statusCode(), response.statusText()),
        level);
  }

  @Override
  public void close() {
    client.close();
  }
}
