package com.github.jelatinone.policy;

import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Document;

public interface RequestPolicyContext<Doc extends Document<Doc>> extends PolicyContext<Doc> {

  long envelopeSchemaVersion();

  ExecutionStage envelopeStage();

  UUID envelopeTargetId();

  UUID envelopeReviewId();
}
