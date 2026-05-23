package com.github.jelatinone.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.queue.QueueEnvelope;

class MockContractsTest {

  private static final UUID KEY = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void mockStore_tracksValues_andCloseState() {
    MockStore<UUID, String> store = new MockStore<>();

    store.put(KEY, "stored");

    assertEquals("stored", store.query(new Singular<>(Criteria.identifier(KEY))).orElseThrow());
    assertTrue(store.containsKey(KEY));
    assertEquals(Map.of(KEY, "stored"), store.snapshot());

    store.delete(new Singular<>(Criteria.identifier(KEY)));
    store.close();

    assertTrue(store.query(new Singular<>(Criteria.identifier(KEY))).isEmpty());
    assertTrue(store.query(new Several<>(Criteria.identifier(KEY), 1)).isEmpty());
    assertEquals(0, store.size());
    assertTrue(store.isClosed());
  }

  @Test
  void mockQueue_tracksInputOutputRetryAndErrorChannels() {
    MockQueue<String> queue = new MockQueue<>(List.of("alpha", "beta"));

    List<QueueEnvelope<String>> firstPoll = queue.query(new Several<>(MockQueue.ANY, 1)).stream().toList();
    QueueEnvelope<String> alpha = firstPoll.get(0);
    alpha.acknowledgement().retry();
    queue.queue(MockQueue.ANY, "gamma");

    QueueEnvelope<String> beta = queue.query(new Singular<>(MockQueue.ANY)).orElseThrow();
    beta.acknowledgement().error();
    queue.error("delta");

    List<QueueEnvelope<String>> thirdPoll = queue.query(new Several<>(MockQueue.ANY, 1)).stream().toList();

    assertEquals(List.of("alpha"), firstPoll.stream().map(QueueEnvelope::content).toList());
    assertEquals("beta", beta.content());
    assertTrue(thirdPoll.isEmpty());
    assertEquals(List.of("gamma"), queue.sentMessages());
    assertEquals(List.of("alpha"), queue.retryMessages());
    assertEquals(List.of("beta", "delta"), queue.errorMessages());

    queue.close();

    assertTrue(queue.isClosed());
  }
}
