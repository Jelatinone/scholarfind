package com.github.jelatinone.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.queue.QueueException;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.QueueState;
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
    Envelope<String> envelope = new Envelope<>("payload", acknowledgement);

    assertEquals("payload", envelope.content());
    assertSame(acknowledgement, envelope.acknowledgement());
  }

  @Test
  void queueContractsExposeMessagesAndState() {
    QueueResult<String> result = new QueueResult<>(List.of(new Envelope<>("a", new NoopAcknowledgement())), QueueState.ACTIVE);

    assertEquals(1, result.messages().size());
    assertEquals(QueueState.ACTIVE, result.state());
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
