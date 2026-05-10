package com.github.jelatinone.infra.aws.store;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Index;
import com.github.jelatinone.api.Property;

import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public interface DynamoCriteria<Key> extends Criteria<StoreLocation<Key>> {

  Consumer<GetItemRequest.Builder> getItemRequest();

  Consumer<DeleteItemRequest.Builder> deleteItemRequest();

  Consumer<Get.Builder> transactGet();

  Consumer<Delete.Builder> transactDelete();

  static <Key> DynamoCriteria<Key> location(String tableName, Key value) {
    return new DefaultDynamoCriteria<>(
        Optional.of(new StoreLocation<>(tableName, value)),
        Optional.empty(),
        Map.of(),
        noop(),
        noop(),
        noop(),
        noop());
  }

  @Override
  default DynamoCriteria<Key> withProperty(Index<?> key, Property value) {
    Map<Index<?>, Property> next = new HashMap<>(queryProperties());
    next.put(key, value);
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        Map.copyOf(next),
        getItemRequest(),
        deleteItemRequest(),
        transactGet(),
        transactDelete());
  }

  default DynamoCriteria<Key> withGetItemMutation(Consumer<GetItemRequest.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        queryProperties(),
        getItemRequest().andThen(mutator),
        deleteItemRequest(),
        transactGet(),
        transactDelete());
  }

  default DynamoCriteria<Key> withDeleteItemMutation(Consumer<DeleteItemRequest.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        queryProperties(),
        getItemRequest(),
        deleteItemRequest().andThen(mutator),
        transactGet(),
        transactDelete());
  }

  default DynamoCriteria<Key> withTransactGetMutation(Consumer<Get.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        queryProperties(),
        getItemRequest(),
        deleteItemRequest(),
        transactGet().andThen(mutator),
        transactDelete());
  }

  default DynamoCriteria<Key> withTransactDeleteMutation(Consumer<Delete.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        queryProperties(),
        getItemRequest(),
        deleteItemRequest(),
        transactGet(),
        transactDelete().andThen(mutator));
  }

  private static <Builder> Consumer<Builder> noop() {
    return builder -> {
    };
  }
}

record DefaultDynamoCriteria<Key>(
    Optional<StoreLocation<Key>> identifier,
    Optional<Duration> duration,
    Map<Index<?>, Property> queryProperties,
    Consumer<GetItemRequest.Builder> getItemRequest,
    Consumer<DeleteItemRequest.Builder> deleteItemRequest,
    Consumer<Get.Builder> transactGet,
    Consumer<Delete.Builder> transactDelete) implements DynamoCriteria<Key> {

  public DefaultDynamoCriteria {
    queryProperties = queryProperties == null ? Map.of() : Map.copyOf(queryProperties);
    getItemRequest = getItemRequest == null ? builder -> {
    } : getItemRequest;
    deleteItemRequest = deleteItemRequest == null ? builder -> {
    } : deleteItemRequest;
    transactGet = transactGet == null ? builder -> {
    } : transactGet;
    transactDelete = transactDelete == null ? builder -> {
    } : transactDelete;
  }
}
