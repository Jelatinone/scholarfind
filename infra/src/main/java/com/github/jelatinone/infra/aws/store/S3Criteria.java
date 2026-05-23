package com.github.jelatinone.infra.aws.store;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.model.struct.Identity;

import lombok.NonNull;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public interface S3Criteria<Key extends Identity> extends Criteria<StoreLocation<Key>> {

  List<Mutation> mutations();

  sealed interface Mutation permits Head, Get, Delete {
  }

  record Head(Consumer<HeadObjectRequest.Builder> mutator) implements Mutation {
    public Head {
      mutator = mutator == null ? builder -> {
      } : mutator;
    }
  }

  record Get(Consumer<GetObjectRequest.Builder> mutator) implements Mutation {
    public Get {
      mutator = mutator == null ? builder -> {
      } : mutator;
    }
  }

  record Delete(Consumer<DeleteObjectRequest.Builder> mutator) implements Mutation {
    public Delete {
      mutator = mutator == null ? builder -> {
      } : mutator;
    }
  }

  static <Key extends Identity> S3Criteria<Key> location(String tableName, Key value) {
    return new DefaultS3Criteria<>(
        Optional.of(new StoreLocation<>(tableName, value)),
        Optional.empty(),
        List.of());
  }

  default S3Criteria<Key> with(Mutation mutation) {
    List<Mutation> next = new ArrayList<>(mutations());
    next.add(Objects.requireNonNull(mutation));
    return new DefaultS3Criteria<>(identifier(), duration(), next);
  }

  default S3Criteria<Key> withHeadObjectMutation(Consumer<HeadObjectRequest.Builder> mutator) {
    return with(new Head(mutator));
  }

  default S3Criteria<Key> withGetObjectMutation(Consumer<GetObjectRequest.Builder> mutator) {
    return with(new Get(mutator));
  }

  default S3Criteria<Key> withDeleteObjectMutation(Consumer<DeleteObjectRequest.Builder> mutator) {
    return with(new Delete(mutator));
  }
}

record DefaultS3Criteria<Key extends Identity>(
    @NonNull Optional<StoreLocation<Key>> identifier,
    @NonNull Optional<Duration> duration,

    @NonNull List<S3Criteria.Mutation> mutations) implements S3Criteria<Key> {
}
