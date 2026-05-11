package com.github.jelatinone.infra.aws.store;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Criteria;

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
        null,
        null,
        null);
  }

  default S3Criteria<Key> withHeadObjectMutation(Consumer<HeadObjectRequest.Builder> mutator) {
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        headObjectRequest().andThen(mutator),
        getObjectRequest(),
        deleteObjectRequest());
  }

  default S3Criteria<Key> withGetObjectMutation(Consumer<GetObjectRequest.Builder> mutator) {
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        headObjectRequest(),
        getObjectRequest().andThen(mutator),
        deleteObjectRequest());
  }

  default S3Criteria<Key> withDeleteObjectMutation(Consumer<DeleteObjectRequest.Builder> mutator) {
    return new DefaultS3Criteria<>(
        identifier(),
        duration(),
        headObjectRequest(),
        getObjectRequest(),
        deleteObjectRequest().andThen(mutator));
  }
}

record DefaultS3Criteria<Key>(
    Optional<StoreLocation<Key>> identifier,
    Optional<Duration> duration,
    Consumer<HeadObjectRequest.Builder> headObjectRequest,
    Consumer<GetObjectRequest.Builder> getObjectRequest,
    Consumer<DeleteObjectRequest.Builder> deleteObjectRequest) implements S3Criteria<Key> {

  public DefaultS3Criteria {
    headObjectRequest = headObjectRequest == null ? builder -> {
    } : headObjectRequest;
    getObjectRequest = getObjectRequest == null ? builder -> {
    } : getObjectRequest;
    deleteObjectRequest = deleteObjectRequest == null ? builder -> {
    } : deleteObjectRequest;
  }
}
