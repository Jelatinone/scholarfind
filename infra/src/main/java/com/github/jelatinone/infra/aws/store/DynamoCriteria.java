package com.github.jelatinone.infra.aws.store;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Criteria;

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
        null,
        null,
        null,
        null);
  }

  default DynamoCriteria<Key> withGetItemMutation(Consumer<GetItemRequest.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        getItemRequest().andThen(mutator),
        deleteItemRequest(),
        transactGet(),
        transactDelete());
  }

  default DynamoCriteria<Key> withDeleteItemMutation(Consumer<DeleteItemRequest.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        getItemRequest(),
        deleteItemRequest().andThen(mutator),
        transactGet(),
        transactDelete());
  }

  default DynamoCriteria<Key> withTransactGetMutation(Consumer<Get.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        getItemRequest(),
        deleteItemRequest(),
        transactGet().andThen(mutator),
        transactDelete());
  }

  default DynamoCriteria<Key> withTransactDeleteMutation(Consumer<Delete.Builder> mutator) {
    return new DefaultDynamoCriteria<>(
        identifier(),
        duration(),
        getItemRequest(),
        deleteItemRequest(),
        transactGet(),
        transactDelete().andThen(mutator));
  }
}

record DefaultDynamoCriteria<Key>(
    Optional<StoreLocation<Key>> identifier,
    Optional<Duration> duration,
    Consumer<GetItemRequest.Builder> getItemRequest,
    Consumer<DeleteItemRequest.Builder> deleteItemRequest,
    Consumer<Get.Builder> transactGet,
    Consumer<Delete.Builder> transactDelete) implements DynamoCriteria<Key> {

  public DefaultDynamoCriteria {
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
