package com.github.scholarfind.policy;

import java.util.UUID;

import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.shared.StageDocument;

public interface RequestPolicyContext<D extends StageDocument<D>> extends PolicyContext<D> {

  long envelopeSchemaVersion();

  ProcessingStage envelopeStage();

  UUID envelopeRequestId();

  UUID envelopeTargetId();
}
