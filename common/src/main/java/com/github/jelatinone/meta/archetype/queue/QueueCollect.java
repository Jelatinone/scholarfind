package com.github.jelatinone.meta.archetype.queue;

import java.util.List;

import com.github.jelatinone.api.Envelope;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.meta.archetype.Collect;
import com.github.jelatinone.meta.result.CollectionResult;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueCollect<Consumes> implements Collect<Envelope<Consumes>> {

  Configuration<Consumes> config;

  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  public static final class Configuration<Consumes> {

    @Builder.Default
    int collectionSize = 10;

    RetryableQueue<Consumes> collectionSource;
  }

  @Override
  public CollectionResult<Envelope<Consumes>> collect() {
    QueueResult<Consumes> result = config.collectionSource.poll(config.collectionSize);
    List<Envelope<Consumes>> envelopes = result.messages();
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
