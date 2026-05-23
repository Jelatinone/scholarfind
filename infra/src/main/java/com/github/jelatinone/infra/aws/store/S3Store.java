package com.github.jelatinone.infra.aws.store;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.api.store.StoreException;
import com.github.jelatinone.infra.aws.AWSInfrastructure;
import com.github.jelatinone.infra.aws.store.serial.S3Serializer;
import com.github.jelatinone.model.struct.Identity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class S3Store<Key extends Identity, Value>
    implements Store<StoreLocation<Key>, Value, S3Criteria<Key>>,
    AWSInfrastructure<CreateBucketRequest, CreateBucketResponse> {

  S3Client client;
  S3Serializer<Value, Key> serializer;

  @Override
  public Optional<CreateBucketResponse> tryCreate(CreateBucketRequest create) {
    try {
      return Optional.of(client.createBucket(create));
    } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException exception) {
      return Optional.empty();
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public void put(StoreLocation<Key> location, Value body) {
    put(location, body, builder -> {
    });
  }

  public void put(
      StoreLocation<Key> location,
      Value body,
      Consumer<PutObjectRequest.Builder> mutator) {
    try {
      S3Serializer.EncodedValue<Key> encoded = serializer.encode(location.value(), body);
      PutObjectRequest.Builder builder = PutObjectRequest.builder()
          .bucket(location.tableName())
          .key(serializer.encodeKey(encoded.key()))
          .metadata(encoded.metadata())
          .applyMutation(mutator);
      if (encoded.contentType() != null && !encoded.contentType().isBlank()) {
        builder.contentType(encoded.contentType());
      }
      if (encoded.contentEncoding() != null && !encoded.contentEncoding().isBlank()) {
        builder.contentEncoding(encoded.contentEncoding());
      }
      PutObjectResponse response = client.putObject(builder.build(), RequestBody.fromBytes(encoded.body()));
      if (response.sdkHttpResponse() != null && !response.sdkHttpResponse().isSuccessful()) {
        throw new StoreException.RetryStoreException("Failed to persist S3 store item", null);
      }
    } catch (StoreException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException("Failed to encode store item", exception);
    }
  }

  @Override
  public boolean query(Exists<S3Criteria<Key>> query) {
    return query(new Count<>(query.criteria())) > 0;
  }

  @Override
  public long query(Count<S3Criteria<Key>> query) {
    try {
      S3Criteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      client.headObject(builder -> builder
          .bucket(location.tableName())
          .key(serializer.encodeKey(location.value()))
          .applyMutation(headObject(criteria)));
      return 1L;
    } catch (NoSuchKeyException exception) {
      return 0L;
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        return 0L;
      }
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public Optional<Value> query(Singular<S3Criteria<Key>> query) {
    try {
      S3Criteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(builder -> builder
          .bucket(location.tableName())
          .key(serializer.encodeKey(location.value()))
          .applyMutation(getObject(criteria)));
      GetObjectResponse response = object.response();
      return Optional.ofNullable(serializer.decode(new S3Serializer.StoredValue<>(
          location.value(),
          object.asByteArray(),
          response.contentType(),
          response.contentEncoding(),
          response.metadata())));
    } catch (NoSuchKeyException exception) {
      return Optional.empty();
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        return Optional.empty();
      }
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public Collection<Value> query(Several<S3Criteria<Key>> query) {
    return query(new Singular<>(query.criteria()))
        .map(List::of)
        .orElseGet(List::of);
  }

  @Override
  public void delete(Singular<S3Criteria<Key>> query) {
    try {
      S3Criteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      DeleteObjectResponse response = client.deleteObject(builder -> builder
          .bucket(location.tableName())
          .key(serializer.encodeKey(location.value()))
          .applyMutation(deleteObject(criteria)));
      if (response.sdkHttpResponse() != null && !response.sdkHttpResponse().isSuccessful()) {
        throw new StoreException.RetryStoreException("Failed to delete S3 store item", null);
      }
    } catch (StoreException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public void delete(Several<S3Criteria<Key>> query) {
    delete(new Singular<>(query.criteria()));
  }

  @Override
  public void close() {
    client.close();
  }

  private StoreLocation<Key> location(S3Criteria<Key> criteria) {
    return criteria.identifier().orElseThrow();
  }

  private Consumer<HeadObjectRequest.Builder> headObject(S3Criteria<Key> criteria) {
    Consumer<HeadObjectRequest.Builder> result = builder -> {
    };
    for (S3Criteria.Mutation<?> mutation : criteria.mutations()) {
      result = switch (mutation) {
        case S3Criteria.Head headObject -> result.andThen(headObject.mutator());
        case S3Criteria.Get ignored -> result;
        case S3Criteria.Delete ignored -> result;
      };
    }
    return result;
  }

  private Consumer<GetObjectRequest.Builder> getObject(S3Criteria<Key> criteria) {
    Consumer<GetObjectRequest.Builder> result = builder -> {
    };
    for (S3Criteria.Mutation<?> mutation : criteria.mutations()) {
      result = switch (mutation) {
        case S3Criteria.Head ignored -> result;
        case S3Criteria.Get getObject -> result.andThen(getObject.mutator());
        case S3Criteria.Delete ignored -> result;
      };
    }
    return result;
  }

  private Consumer<DeleteObjectRequest.Builder> deleteObject(S3Criteria<Key> criteria) {
    Consumer<DeleteObjectRequest.Builder> result = builder -> {
    };
    for (S3Criteria.Mutation<?> mutation : criteria.mutations()) {
      result = switch (mutation) {
        case S3Criteria.Head ignored -> result;
        case S3Criteria.Get ignored -> result;
        case S3Criteria.Delete deleteObject -> result.andThen(deleteObject.mutator());
      };
    }
    return result;
  }
}
