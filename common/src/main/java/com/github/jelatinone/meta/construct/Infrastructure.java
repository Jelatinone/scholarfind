package com.github.jelatinone.meta.construct;

import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.models.audit.AttemptEvent;
import com.github.jelatinone.models.audit.StageExecution;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.StageEnvelope;

public interface Infrastructure<In extends Request, Out extends Request> extends AutoCloseable {

  RetryableQueue<StageEnvelope<In>> input();

  Router output();

  Store<AttemptEvent, String> eventStore();

  Store<StageExecution, String> executionStore();
}
