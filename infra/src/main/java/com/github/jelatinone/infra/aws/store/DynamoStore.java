package com.github.jelatinone.infra.aws.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.api.store.StoreException;
import com.github.jelatinone.infra.aws.AWSInfrastructure;
import com.github.jelatinone.infra.aws.store.serial.DynamoSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DynamoStore<Key, Value>
    implements Store<StoreLocation<Key>, Value, DynamoCriteria<Key>>,
    AWSInfrastructure<CreateTableRequest, CreateTableResponse> {

  DynamoDbClient client;
  DynamoSerializer<Value, Key> serializer;

  static Logger _logger = LoggerFactory.getLogger(DynamoStore.class);

  @Override
  public Optional<CreateTableResponse> tryCreate(CreateTableRequest create) {
    try {
      CreateTableResponse response = client.createTable(create);
      response("create table", response.sdkHttpResponse());
      return Optional.of(response);
    } catch (ResourceInUseException exception) {
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
      Consumer<software.amazon.awssdk.services.dynamodb.model.PutItemRequest.Builder> mutator) {
    try {
      PutItemResponse response = client.putItem(builder -> builder
          .tableName(location.tableName())
          .item(encode(location.value(), body))
          .applyMutation(mutator));
      response("put item", response.sdkHttpResponse());
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException("Failed to encode store item", exception);
    }
  }

  @Override
  public boolean query(Exists<DynamoCriteria<Key>> query) {
    return query(new Count<>(query.criteria())) > 0;
  }

  @Override
  public long query(Count<DynamoCriteria<Key>> query) {
    try {
      DynamoCriteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      GetItemResponse response = client.getItem(builder -> builder
          .tableName(location.tableName())
          .key(serializer.encodeKey(location.value()))
          .applyMutation(criteria.getItemRequest()));
      response("count item", response.sdkHttpResponse());
      return response.item() == null || response.item().isEmpty() ? 0L : 1L;
    } catch (Exception exception) {
      _logger.error(String.format("Store query failed : %s", exception.getMessage()));
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public Optional<Value> query(Singular<DynamoCriteria<Key>> query) {
    try {
      DynamoCriteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      GetItemResponse response = client.getItem(builder -> builder
          .tableName(location.tableName())
          .key(serializer.encodeKey(location.value()))
          .applyMutation(criteria.getItemRequest()));
      response("get item", response.sdkHttpResponse());
      if (response.item() == null || response.item().isEmpty()) {
        return null;
      }
      return Optional.ofNullable(serializer.decodeItem(response.item()));
    } catch (Exception exception) {
      _logger.error(String.format("Store query failed : %s", exception.getMessage()));
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public Collection<Value> query(Several<DynamoCriteria<Key>> query) {
    return query(new Singular<>(query.criteria()))
        .map(value -> value == null
            ? List.<Value>of()
            : List.of(value))
        .get();
  }

  @Override
  public void delete(Singular<DynamoCriteria<Key>> query) {
    try {
      DynamoCriteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      DeleteItemResponse response = client.deleteItem(builder -> builder
          .tableName(location.tableName())
          .key(serializer.encodeKey(location.value()))
          .applyMutation(criteria.deleteItemRequest()));
      response("delete item", response.sdkHttpResponse());
    } catch (Exception exception) {
      _logger.error(String.format("Store delete failed : %s", exception.getMessage()));
      throw new StoreException.RetryStoreException(exception.getMessage(), exception);
    }
  }

  @Override
  public void delete(Several<DynamoCriteria<Key>> query) {
    delete(new Singular<>(query.criteria()));
  }

  public TransactWriteItem transactPut(StoreLocation<Key> location, Value body) {
    return transactPut(location, body, builder -> {
    });
  }

  public TransactWriteItem transactPut(
      StoreLocation<Key> location,
      Value body,
      Consumer<Put.Builder> mutator) {
    try {
      return TransactWriteItem.builder()
          .put(Put.builder()
              .tableName(location.tableName())
              .item(encode(location.value(), body))
              .applyMutation(mutator)
              .build())
          .build();
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException("Failed to transact store put item", exception);
    }
  }

  public TransactGetItem transactQuery(Singular<DynamoCriteria<Key>> query) {
    return transactQuery(query, builder -> {
    });
  }

  public TransactGetItem transactQuery(Singular<DynamoCriteria<Key>> query, Consumer<Get.Builder> mutator) {
    try {
      DynamoCriteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      return TransactGetItem.builder()
          .get(Get.builder()
              .tableName(location.tableName())
              .key(serializer.encodeKey(location.value()))
              .applyMutation(criteria.transactGet().andThen(mutator))
              .build())
          .build();
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException("Failed to transact get store item", exception);
    }
  }

  public TransactWriteItem transactDelete(Singular<DynamoCriteria<Key>> query) {
    return transactDelete(query, builder -> {
    });
  }

  public TransactWriteItem transactDelete(Singular<DynamoCriteria<Key>> query, Consumer<Delete.Builder> mutator) {
    try {
      DynamoCriteria<Key> criteria = query.criteria();
      StoreLocation<Key> location = location(criteria);
      return TransactWriteItem.builder()
          .delete(Delete.builder()
              .tableName(location.tableName())
              .key(serializer.encodeKey(location.value()))
              .applyMutation(criteria.transactDelete().andThen(mutator))
              .build())
          .build();
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException("Failed to transact delete store item", exception);
    }
  }

  public Map<String, AttributeValue> encode(Key key, Value body) {
    try {
      return serializer.encodeItem(key, body);
    } catch (Exception exception) {
      throw new StoreException.RetryStoreException("Failed to encode store item", exception);
    }
  }

  private StoreLocation<Key> location(DynamoCriteria<Key> criteria) {
    return criteria.identifier().orElseThrow();
  }

  private void response(String action, SdkHttpResponse response) {
    if (response == null) {
      return;
    }
    String message = String.format("%s completed : [%d] %s", action, response.statusCode(), response.statusText());
    if (response.isSuccessful()) {
      _logger.info(message);
      return;
    }
    _logger.error(message);
  }

  @Override
  public void close() {
    client.close();
  }
}
