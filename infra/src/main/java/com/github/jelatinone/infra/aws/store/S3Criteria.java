package com.github.jelatinone.infra.aws.store;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Index;
import com.github.jelatinone.api.Property;

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public interface S3Criteria<Key> extends Criteria<StoreLocation<Key>> {

  Consumer<HeadObjectRequest.Builder> headObjectRequest();

  Consumer<GetObjectRequest.Builder> getObjectRequest();

  Consumer<DeleteObjectRequest.Builder> deleteObjectRequest();

  static <Key> S3Criteria<Key> location(String tableName, Key value) {
    return new DefaultS3Criteria<>(
        Optional.of(new StoreLocation<>(tableName, value)),
        Optional.empty(),
        Map.of(),
        noop(),
        noop(),
        noop());
  }

  @Override
  default S3Criteria<Key> withProperty(Index<?> key, Property value) {
    Map<Index<?>, Property> next = new HashMap<>(queryProperties());
    next.put(key, value);
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        Map.copyOf(next),
        headObjectRequest(),
        getObjectRequest(),
        deleteObjectRequest());
  }

  default S3Criteria<Key> withHeadObjectMutation(Consumer<HeadObjectRequest.Builder> mutator) {
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        queryProperties(),
        headObjectRequest().andThen(mutator),
        getObjectRequest(),
        deleteObjectRequest());
  }

  default S3Criteria<Key> withGetObjectMutation(Consumer<GetObjectRequest.Builder> mutator) {
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        queryProperties(),
        headObjectRequest(),
        getObjectRequest().andThen(mutator),
        deleteObjectRequest());
  }

  default S3Criteria<Key> withDeleteObjectMutation(Consumer<DeleteObjectRequest.Builder> mutator) {
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        queryProperties(),
        headObjectRequest(),
        getObjectRequest(),
        deleteObjectRequest().andThen(mutator));
  }

  private static <Builder> Consumer<Builder> noop() {
    return builder -> {
    };
  }
}

record DefaultS3Criteria<Key>(
    Optional<StoreLocation<Key>> identifier,
    Optional<Duration> duration,
    Map<Index<?>, Property> queryProperties,
    Consumer<HeadObjectRequest.Builder> headObjectRequest,
    Consumer<GetObjectRequest.Builder> getObjectRequest,
    Consumer<DeleteObjectRequest.Builder> deleteObjectRequest) implements S3Criteria<Key> {

  public DefaultS3Criteria {
    queryProperties = queryProperties == null ? Map.of() : Map.copyOf(queryProperties);
    headObjectRequest = headObjectRequest == null ? builder -> {
    } : headObjectRequest;
    getObjectRequest = getObjectRequest == null ? builder -> {
    } : getObjectRequest;
    deleteObjectRequest = deleteObjectRequest == null ? builder -> {
    } : deleteObjectRequest;
  }
}
