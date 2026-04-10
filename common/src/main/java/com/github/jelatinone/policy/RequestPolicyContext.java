package com.github.jelatinone.policy;

import java.util.UUID;

import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.shared.StageDocument;

public interface RequestPolicyContext<Document extends StageDocument<Document>> extends PolicyContext<Document> {

	long envelopeSchemaVersion();

	ProcessingStage envelopeStage();

	UUID envelopeRequestId();

	UUID envelopeTargetId();
}
