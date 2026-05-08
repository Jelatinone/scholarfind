package com.github.jelatinone.meta.construct;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.struct.Request;

public interface Infrastructure<In extends Request, Out extends Request, Queryable> extends AutoCloseable {

	Router router();

	Store<AttemptEvent, String> attemptStore();

	Store<ExecutionEvent, String> executionStore();
}
