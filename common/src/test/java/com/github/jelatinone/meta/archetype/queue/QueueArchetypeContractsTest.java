package com.github.jelatinone.meta.archetype.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Acknowledgement;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.mock.MockQueue;
import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.PostResult;
import com.github.jelatinone.meta.transitory.Directive;

class QueueArchetypeContractsTest {

  @SuppressWarnings("resource")
  @Test
  void queueCollect_mapsSeveralQueryResults() {
    MockQueue<String> queue = new MockQueue<String>().addInput("a");
    QueueCollect<String, ?> collect = new QueueCollect<>(QueueCollect.Configuration
        .<String, Void>builder()
        .collectionSource(queue)
        .collectionQuery(new Several<>(MockQueue.ANY, 1))
        .build());

    CollectionResult.Alive<?> result = assertInstanceOf(CollectionResult.Alive.class, collect.collect());

    assertEquals(List.of("a"), result.collection().stream()
        .map(QueueEnvelope.class::cast)
        .map(QueueEnvelope::content)
        .toList());
  }

  @Test
  void queueOperation_wrapsSuccessAndRecoveryResults() {
    QueueOperation<String, Integer> operation = new QueueOperation<>(
        String::length,
        (value, throwable) -> -1);

    QueueEnvelope<Integer> success = operation.operate(new QueueEnvelope<>("abcd", new NoopAcknowledgement()));
    QueueEnvelope<Integer> failure = operation.operate(new QueueEnvelope<>(null, new NoopAcknowledgement()));

    assertEquals(4, success.content());
    assertEquals(-1, failure.content());
  }

  @Test
  void queuePersist_routesDirective_andAcknowledgesEnvelope() {
    AtomicInteger successCount = new AtomicInteger();
    List<String> callbacks = new ArrayList<>();
    QueuePersist<String> persist = new QueuePersist<>(new QueueDisposition<>() {
      @Override
      public Directive directive(String output) {
        return Directive.COMPLETE;
      }

      @Override
      public void complete(String output) {
        callbacks.add("complete:" + output);
      }

      @Override
      public void retry(String output) {
        callbacks.add("retry:" + output);
      }

      @Override
      public void error(String output) {
        callbacks.add("error:" + output);
      }
    });
    QueueEnvelope<String> envelope = new QueueEnvelope<>("done", new Acknowledgement() {
      @Override
      public void success() {
        successCount.incrementAndGet();
      }

      @Override
      public void retry() {
      }

      @Override
      public void error() {
      }
    });

    PostResult result = persist.post(envelope);

    assertInstanceOf(PostResult.Success.class, result);
    assertEquals(List.of("complete:done"), callbacks);
    assertEquals(1, successCount.get());
    assertInstanceOf(PostResult.Retry.class, persist.post(null));
  }

  private static final class NoopAcknowledgement implements Acknowledgement {
    @Override
    public void success() {
    }

    @Override
    public void retry() {
    }

    @Override
    public void error() {
    }
  }
}
