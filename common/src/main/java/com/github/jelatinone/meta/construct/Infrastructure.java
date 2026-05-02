package com.github.jelatinone.meta.construct;

import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

public interface Infrastructure<In extends Request, Out extends Request> extends AutoCloseable {

	RetryableQueue<Letter<In>> input();

	Router router();

	Store<AttemptEvent, String> attemptStore();

	Store<ExecutionEvent, String> executionStore();
}
