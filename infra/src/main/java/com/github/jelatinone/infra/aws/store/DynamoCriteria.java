package com.github.jelatinone.infra.aws.store;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Criteria;

import lombok.NonNull;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public interface DynamoCriteria<Key> extends Criteria<StoreLocation<Key>> {

  List<Mutation<?>> mutations();

  sealed interface Mutation<Request> permits Command, Transact {

    Consumer<Request> mutator();
  }

  record Command<Request>(
      Consumer<Request> mutator) implements Mutation<Request> {
    public Command {

      mutator = mutator == null ? builder -> {
      } : mutator;
    }
  }

  record Transact<Request>(
      Consumer<Request> mutator) implements Mutation<Request> {
    public Transact {
      mutator = mutator == null ? builder -> {
      } : mutator;
    }
  }

  static <Key> DynamoCriteria<Key> location(String tableName, Key value) {
    return new DefaultDynamoCriteria<>(
        Optional.of(new StoreLocation<>(tableName, value)),
        Optional.empty(),
        List.of());
  }

  default DynamoCriteria<Key> withMutation(Mutation<?> mutation) {
    List<Mutation<?>> next = new ArrayList<>(mutations());
    next.add(Objects.requireNonNull(mutation));
    return new DefaultDynamoCriteria<>(identifier(), duration(), next);
  }

  default <Request> DynamoCriteria<Key> withCommandMutation(
      Consumer<Request> mutator) {
    return withMutation(new Command<>(mutator));
  }

  default <Request> DynamoCriteria<Key> withTransactMutation(
      Consumer<Request> mutator) {
    return withMutation(new Transact<>(mutator));
  }

  default DynamoCriteria<Key> withGetItemMutation(Consumer<GetItemRequest.Builder> mutator) {
    return withCommandMutation(mutator);
  }

  default DynamoCriteria<Key> withDeleteItemMutation(Consumer<DeleteItemRequest.Builder> mutator) {
    return withCommandMutation(mutator);
  }

  default DynamoCriteria<Key> withTransactGetMutation(Consumer<Get.Builder> mutator) {
    return withTransactMutation(mutator);
  }

  default DynamoCriteria<Key> withTransactDeleteMutation(Consumer<Delete.Builder> mutator) {
    return withTransactMutation(mutator);
  }
}

record DefaultDynamoCriteria<Key>(
    @NonNull Optional<StoreLocation<Key>> identifier,
    @NonNull Optional<Duration> duration,

    @NonNull List<DynamoCriteria.Mutation<?>> mutations) implements DynamoCriteria<Key> {

}
