package com.github.jelatinone.meta.construct;

import java.util.UUID;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.struct.Document;

public interface Infrastructure<Documents extends Document<Documents>> extends AutoCloseable {

	Router router();

	Store<UUID, Documents, Criteria<UUID>> documentStore();

	Store<UUID, AttemptEvent, Criteria<UUID>> attemptStore();

	Store<UUID, ExecutionEvent, Criteria<UUID>> executionStore();
}
