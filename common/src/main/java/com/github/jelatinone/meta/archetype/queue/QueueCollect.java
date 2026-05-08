package com.github.jelatinone.meta.archetype.queue;

import java.util.List;

import com.github.jelatinone.api.Query;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.infra.queue.RetryableQueue;
import com.github.jelatinone.meta.archetype.Collect;
import com.github.jelatinone.meta.result.CollectionResult;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueCollect<Consumes, Queryable> implements Collect<QueueEnvelope<Consumes>> {

  Configuration<Consumes, Queryable> config;

  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  public static final class Configuration<Consumes, Queryable> {

    Query.Several<Queryable> collectionQuery;

    RetryableQueue<Consumes, Queryable> collectionSource;
  }

  @Override
  public CollectionResult<QueueEnvelope<Consumes>> collect() {
    QueueResult<Consumes> result = config.collectionSource.query(config.collectionQuery);
    List<QueueEnvelope<Consumes>> envelopes = result.messages();
    if (!envelopes.isEmpty()) {
      return new CollectionResult.Alive<>(envelopes);
    }
    return switch (result.state()) {
      case ACTIVE -> new CollectionResult.Alive<>(List.of());
      case IDLE -> new CollectionResult.Idle<>();
      case EMPTY -> new CollectionResult.Empty<>();
    };
  }

  @Override
  public void close() throws Exception {
    config.collectionSource.close();
  }
}
