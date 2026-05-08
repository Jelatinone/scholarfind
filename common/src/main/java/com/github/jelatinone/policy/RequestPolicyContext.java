package com.github.jelatinone.policy;

import java.util.UUID;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Document;

public interface RequestPolicyContext<Documents extends Document<Documents>> extends PolicyContext<Documents> {

  long envelopeSchemaVersion();

  UUID envelopeTargetId();

  UUID envelopeReviewId();

  ExecutionStage envelopeStage();
}
