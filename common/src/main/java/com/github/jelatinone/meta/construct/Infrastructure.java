package com.github.jelatinone.meta.construct;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public interface Infrastructure<Documents extends Document<Documents>> extends AutoCloseable {

  Router router();

  Store<TargetIdentity, Documents, Criteria<TargetIdentity>> documentStore();

  Store<TargetIdentity, AttemptEvent, Criteria<TargetIdentity>> attemptStore();

  Store<TargetIdentity, ExecutionEvent, Criteria<TargetIdentity>> executionStore();
}
