package com.github.jelatinone.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.mock.MockStore;
import com.github.jelatinone.meta.construct.Infrastructure;
import com.github.jelatinone.meta.construct.Router;
import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.PostResult;
import com.github.jelatinone.meta.transitory.Directive;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;
import com.github.jelatinone.utility.scheduler.ConstantBackoffScheduler;

class MetaTaskContractsTest {

  @Test
  void resultAndDirectiveContracts_keepExpectedShapes() {
		CollectionResult<String> alive = new CollectionResult.Alive<>(List.of("a"));
		CollectionResult<String> empty = new CollectionResult.Empty<>();

		assertInstanceOf(CollectionResult.Alive.class, alive);
		assertInstanceOf(CollectionResult.Empty.class, empty);
    assertInstanceOf(PostResult.Success.class, new PostResult.Success());
    assertInstanceOf(PostResult.Retry.class, new PostResult.Retry(new IllegalStateException("retry")));
    assertEquals(Directive.ERROR, Directive.valueOf("ERROR"));
  }

  @SuppressWarnings("resource")
  @Test
  void sequentialTask_processesCollectedWork_toCompletion() {
    TestSequentialTask task = new TestSequentialTask();

    task.run();

    assertEquals(List.of("ALPHA", "BETA"), task.posted);
    assertEquals(SequentialTask.State.COMPLETED, task.state());
  }

  @Test
  void parallelTask_processesCollectedWork_toCompletion() throws Exception {
    TestParallelTask task = new TestParallelTask();

    try {
      task.run();

      task.completable().get(2, TimeUnit.SECONDS);

      assertEquals(ParallelTask.State.COMPLETED, task.state());
      assertTrue(task.posted.containsAll(List.of("ALPHA", "BETA")));
    } finally {
      task.close();
    }
  }

  @SuppressWarnings("resource")
  @Test
  void infrastructureBindsRouterAndStores() {
    Router router = envelope -> {
    };
    MockStore<TargetIdentity, InvestigateDocument> documentStore = new MockStore<>();
    MockStore<TargetIdentity, AttemptEvent> attemptStore = new MockStore<>();
    MockStore<TargetIdentity, ExecutionEvent> executionStore = new MockStore<>();

    Infrastructure<InvestigateDocument> infrastructure = new Infrastructure<>() {
      @Override
      public Router router() {
        return router;
      }

      @Override
      public Store<TargetIdentity, InvestigateDocument, Criteria<TargetIdentity>> documentStore() {
        return documentStore;
      }

      @Override
      public Store<TargetIdentity, AttemptEvent, Criteria<TargetIdentity>> attemptStore() {
        return attemptStore;
      }

      @Override
      public Store<TargetIdentity, ExecutionEvent, Criteria<TargetIdentity>> executionStore() {
        return executionStore;
      }

      @Override
      public void close() {
      }
    };

    assertEquals(router, infrastructure.router());
    assertEquals(documentStore, infrastructure.documentStore());
    assertEquals(attemptStore, infrastructure.attemptStore());
    assertEquals(executionStore, infrastructure.executionStore());
  }

  private static final class TestSequentialTask extends SequentialTask<String, String> {
    private final Queue<CollectionResult<String>> collections = new ArrayDeque<>(
        List.of(new CollectionResult.Alive<>(List.of("alpha", "beta")), new CollectionResult.Empty<>()));
    private final List<String> posted = Collections.synchronizedList(new ArrayList<>());

    private TestSequentialTask() {
      super(Task.Configuration.builder()
          .name("sequential")
          .awaitScheduler(new ConstantBackoffScheduler(0))
          .retryScheduler(new ConstantBackoffScheduler(0))
          .build());
    }

    @Override
    public CollectionResult<String> collect() {
      return collections.remove();
    }

    @Override
    public String operate(String operand) {
      return operand.toUpperCase();
    }

    @Override
    public PostResult post(String operand) {
      posted.add(operand);
      return new PostResult.Success();
    }

    private State state() {
      return _state.get();
    }
  }

  private static final class TestParallelTask extends ParallelTask<String, String> {
    private final Queue<CollectionResult<String>> collections = new ArrayDeque<>(
        List.of(new CollectionResult.Alive<>(List.of("alpha", "beta")), new CollectionResult.Empty<>()));
    private final List<String> posted = Collections.synchronizedList(new ArrayList<>());

    private TestParallelTask() {
      super(
          ParallelTask.Configuration.builder()
              .threadParallelism(2)
              .executor(Executors.newFixedThreadPool(2))
              .build(),
          Task.Configuration.builder()
              .name("parallel")
              .awaitScheduler(new ConstantBackoffScheduler(0))
              .retryScheduler(new ConstantBackoffScheduler(0))
              .build());
    }

    @Override
    public CollectionResult<String> collect() {
      return collections.remove();
    }

    @Override
    public String operate(String operand) {
      return operand.toUpperCase();
    }

    @Override
    public PostResult post(String operand) {
      posted.add(operand);
      return new PostResult.Success();
    }

    private State state() {
      return _state.get();
    }
  }
}
