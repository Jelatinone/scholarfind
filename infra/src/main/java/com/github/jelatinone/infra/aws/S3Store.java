package com.github.jelatinone.infra.aws;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.infra.aws.serial.S3Serializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class S3Store<Value, Key> implements Store<Value, Key> {
  S3Client client;
  String bucket;
  S3Serializer<Value, Key> serializer;

  @Override
  public Key put(Value body) {
    try {
      S3Serializer.EncodedValue<Key> encoded = serializer.encode(body);
      PutObjectRequest.Builder builder = PutObjectRequest.builder()
          .bucket(bucket)
          .key(serializer.encodeKey(encoded.key()))
          .metadata(encoded.metadata());
      if (encoded.contentType() != null && !encoded.contentType().isBlank()) {
        builder.contentType(encoded.contentType());
      }
      if (encoded.contentEncoding() != null && !encoded.contentEncoding().isBlank()) {
        builder.contentEncoding(encoded.contentEncoding());
      }
      PutObjectResponse response = client.putObject(builder.build(), RequestBody.fromBytes(encoded.body()));
      if (response.sdkHttpResponse() != null && !response.sdkHttpResponse().isSuccessful()) {
        throw new IllegalStateException("Failed to persist S3 store item");
      }
      return encoded.key();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to encode store item", exception);
    }
  }

  @Override
  public Value get(Key key) {
    try {
      ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(builder -> builder
          .bucket(bucket)
          .key(serializer.encodeKey(key))
          .build());
      GetObjectResponse response = object.response();
      return serializer.decode(new S3Serializer.StoredValue<>(
          key,
          object.asByteArray(),
          response.contentType(),
          response.contentEncoding(),
          response.metadata()));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to decode store item", exception);
    }
  }

  @Override
  public void delete(Key key) {
    try {
      DeleteObjectResponse response = client.deleteObject(builder -> builder
          .bucket(bucket)
          .key(serializer.encodeKey(key))
          .build());
      if (response.sdkHttpResponse() != null && !response.sdkHttpResponse().isSuccessful()) {
        throw new IllegalStateException("Failed to delete S3 store item");
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to delete store item", exception);
    }
  }

  @Override
  public void close() {
    client.close();
  }

}
