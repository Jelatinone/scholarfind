package com.github.jelatinone.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.QueueState;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.model.transit.RequestIntent;

final class RequestOrchestrationTaskTest {
  private static final Instant NOW = Instant.parse("2026-05-05T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void routesEligibleIntentAndMarksQueued() {
    UUID intentId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    TestRequest request = request(targetId, reviewId);
    Letter<TestRequest> letter = new Letter<>(targetId, reviewId, ExecutionStage.INVESTIGATE, request, NOW);
    FakeIntentSource source = new FakeIntentSource(List.of(new RequestIntent<>(intentId, letter, NOW)));
    FakeQueue queue = new FakeQueue();

    new RequestOrchestrationTask<>(
        source,
        queue,
        ExecutionStage.INVESTIGATE,
        TestRequest.class,
        10,
        CLOCK)
        .run();

    assertEquals(List.of(letter), queue.sent);
    assertEquals(List.of(intentId), source.queued);
    assertTrue(source.failed.isEmpty());
  }

  @Test
  void marksWrongStageIntentFailed() {
    UUID intentId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    TestRequest request = request(targetId, reviewId);
    Letter<TestRequest> letter = new Letter<>(targetId, reviewId, ExecutionStage.ANNOTATE, request, NOW);
    FakeIntentSource source = new FakeIntentSource(List.of(new RequestIntent<>(intentId, letter, NOW)));
    FakeQueue queue = new FakeQueue();

    new RequestOrchestrationTask<>(
        source,
        queue,
        ExecutionStage.INVESTIGATE,
        TestRequest.class,
        10,
        CLOCK)
        .run();

    assertTrue(queue.sent.isEmpty());
    assertTrue(source.queued.isEmpty());
    assertEquals(List.of(intentId), source.failed);
  }

  private static TestRequest request(UUID targetId, UUID reviewId) {
    return new TestRequest(
        new RequestHeader(targetId, reviewId, 0, ExecutionStage.DISCOVERY, NOW),
        targetId,
        reviewId);
  }

  private record TestRequest(
      RequestHeader requestHeader,
      UUID targetId,
      UUID reviewId) implements Request {
  }

  private static final class FakeIntentSource implements RequestIntentSource<TestRequest> {
    private final Collection<RequestIntent<TestRequest>> intents;
    private final List<UUID> queued = new ArrayList<>();
    private final List<UUID> failed = new ArrayList<>();

    private FakeIntentSource(Collection<RequestIntent<TestRequest>> intents) {
      this.intents = intents;
    }

    @Override
    public Collection<RequestIntent<TestRequest>> collectEligible(ExecutionStage executionRef, int limit) {
      return intents;
    }

    @Override
    public void markQueued(UUID intentId, Letter<TestRequest> letter, Instant queuedAt) {
      queued.add(intentId);
    }

    @Override
    public void markFailed(UUID intentId, Letter<TestRequest> letter, Throwable throwable, Instant failedAt) {
      failed.add(intentId);
    }
  }

  private static final class FakeQueue implements com.github.jelatinone.api.queue.Queue<Letter<TestRequest>> {
    private final List<Letter<TestRequest>> sent = new ArrayList<>();

    @Override
    public QueueResult<Letter<TestRequest>> poll(int messageCount) {
      return new QueueResult<>(List.of(), QueueState.EMPTY);
    }

    @Override
    public void send(Letter<TestRequest> message) {
      sent.add(message);
    }

    @Override
    public void close() {
    }
  }
}
