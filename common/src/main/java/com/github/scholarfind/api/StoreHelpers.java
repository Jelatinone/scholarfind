package com.github.scholarfind.api;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.event.Level;

import lombok.NonNull;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

public final class StoreHelpers {
  public static <T> PutItemResponse putItem(final @NonNull DynamoDbClient client, final @NonNull T document,
      final @NonNull String storeLocation,
      final @NonNull Function<T, Map<String, AttributeValue>> mapper, final @NonNull BiConsumer<String, Level> logger) {
    Map<String, AttributeValue> item = mapper.apply(document);
    PutItemRequest putItemRequest = PutItemRequest.builder()
        .item(item)
        .tableName(storeLocation)
        .build();
    PutItemResponse putItemResponse = client.putItem(putItemRequest);
    SdkHttpResponse requestSdkResponse = putItemResponse.sdkHttpResponse();

    if (requestSdkResponse.isSuccessful()) {
      logger.accept(
          String.format("Put item to table [%s] completed : %s", storeLocation, document
              .toString()),
          Level.INFO);
    } else {
      logger.accept(
          String.format("Put item to table [%s] failed : %s", storeLocation, document
              .toString()),
          Level.ERROR);
    }

    return putItemResponse;
  }

  public static DeleteItemResponse deleteItem(final @NonNull DynamoDbClient client, final @NonNull UUID id,
      final String storeLocation, final @NonNull BiConsumer<String, Level> logger) {
    DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
        .tableName(storeLocation)
        .key(Map.of(
            "id", AttributeValue.fromS(id.toString())))
        .build();
    DeleteItemResponse deleteItemResponse = client.deleteItem(deleteItemRequest);
    SdkHttpResponse requestSdkResponse = deleteItemResponse.sdkHttpResponse();

    if (requestSdkResponse.isSuccessful()) {
      logger.accept(
          String.format("Delete item from table [%s] completed : %s", storeLocation, id),
          Level.INFO);
    } else {
      logger.accept(
          String.format("Delete item from table [%s] completed : %s", storeLocation, id),
          Level.ERROR);
    }

    return deleteItemResponse;
  }

  public static <T> T getItem(final @NonNull DynamoDbClient client, final @NonNull UUID id,
      final @NonNull String storeLocation,
      final @NonNull Function<Map<String, AttributeValue>, T> mapper, final @NonNull BiConsumer<String, Level> logger) {
    GetItemRequest getItemRequest = GetItemRequest.builder()
        .tableName(storeLocation)
        .key(Map.of(
            "id", AttributeValue.fromS(id.toString())))
        .build();
    GetItemResponse getItemResponse = client.getItem(getItemRequest);
    SdkHttpResponse requestSdkResponse = getItemResponse.sdkHttpResponse();

    T document = null;
    if (requestSdkResponse.isSuccessful()) {
      Map<String, AttributeValue> item = getItemResponse.item();
      document = mapper.apply(item);
      logger.accept(
          String.format("Retrieve item from table [%s] completed : %s", storeLocation, id),
          Level.INFO);
      return document;
    } else {
      logger.accept(
          String.format("Retrieve item from table [%s] failed : %s", storeLocation, id),
          Level.ERROR);
    }
    return document;
  }
}
