package com.github.jelatinone.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.QueueState;

class MockContractsTest {

  private static final UUID KEY = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void mockStore_tracksValues_andCloseState() {
    MockStore<String, UUID> store = new MockStore<>();

    store.put(KEY, "stored");

    assertEquals("stored", store.get(KEY));
    assertTrue(store.containsKey(KEY));
    assertEquals(Map.of(KEY, "stored"), store.snapshot());

    store.delete(KEY);
    store.close();

    assertEquals(0, store.size());
    assertTrue(store.isClosed());
  }

  @Test
  void mockQueue_tracksInputOutputRetryAndErrorChannels() {
    MockQueue<String> queue = new MockQueue<>(QueueState.IDLE, List.of("alpha", "beta"));

    QueueResult<String> firstPoll = queue.poll(1);
    QueueEnvelope<String> alpha = firstPoll.messages().get(0);
    alpha.acknowledgement().retry();
    queue.send("gamma");

    QueueResult<String> secondPoll = queue.poll(1);
    QueueEnvelope<String> beta = secondPoll.messages().get(0);
    beta.acknowledgement().error();
    queue.sendError("delta");

    QueueResult<String> thirdPoll = queue.poll(1);

    assertEquals(QueueState.ACTIVE, firstPoll.state());
    assertEquals(List.of("alpha"), firstPoll.messages().stream().map(QueueEnvelope::content).toList());
    assertEquals(List.of("beta"), secondPoll.messages().stream().map(QueueEnvelope::content).toList());
    assertTrue(thirdPoll.messages().isEmpty());
    assertEquals(QueueState.IDLE, thirdPoll.state());
    assertEquals(List.of("gamma"), queue.sentMessages());
    assertEquals(List.of("alpha"), queue.retryMessages());
    assertEquals(List.of("beta", "delta"), queue.errorMessages());

    queue.close();

    assertTrue(queue.isClosed());
  }
}
