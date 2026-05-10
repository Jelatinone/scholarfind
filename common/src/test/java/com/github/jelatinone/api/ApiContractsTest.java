package com.github.jelatinone.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.api.queue.QueueException;
import com.github.jelatinone.api.store.StoreException;

class ApiContractsTest {

  @Test
  void envelopeWrapsContentAndAcknowledgement() {
    Acknowledgement acknowledgement = new Acknowledgement() {
      @Override
      public void success() {
      }

      @Override
      public void retry() {
      }

      @Override
      public void error() {
      }
    };
    QueueEnvelope<String> envelope = new QueueEnvelope<>("payload", acknowledgement);

    assertEquals("payload", envelope.content());
    assertSame(acknowledgement, envelope.acknowledgement());
  }

  @Test
  void querySeveralRequiresPositiveLimit() {
    Query.Several<Criteria<Void>> query = new Query.Several<>(Criteria.duration(Duration.ZERO), 1);

    assertEquals(1, query.limit());
  }

  @Test
  void queueAndStoreExceptionsPreserveCause() {
    IOException cause = new IOException("boom");

    RuntimeException retryQueue = new QueueException.RetryQueueException("retry", cause);
    RuntimeException fatalQueue = new QueueException.FatalQueueException("fatal", cause);
    RuntimeException retryStore = new StoreException.RetryStoreException("retry", cause);
    RuntimeException fatalStore = new StoreException.FatalStoreException("fatal", cause);

    assertInstanceOf(QueueException.class, retryQueue);
    assertInstanceOf(QueueException.class, fatalQueue);
    assertInstanceOf(StoreException.class, retryStore);
    assertInstanceOf(StoreException.class, fatalStore);
    assertSame(cause, retryQueue.getCause());
    assertSame(cause, fatalStore.getCause());
  }
}
