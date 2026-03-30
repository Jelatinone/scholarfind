package com.github.scholarfind.task.policy;

import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.ingest.IngestDecision;
import com.github.scholarfind.models.ingest.IngestRequest;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyReason;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.IngestContext;
import com.github.scholarfind.task.IngestState;

public final class RequestSchemaPolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    if (context.envelopeSchemaVersion() != StageEnvelope.SCHEMA_VERSION) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Ingest envelope schema version mismatch");
    }

    if (context.envelopeStage() != ProcessingStage.INGEST) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Ingest envelope stage does not match the ingest pipeline");
    }

    if (context.document().requestHeader() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Ingest request is missing a request header");
    }

    if (context.document().requestHeader().schemaVersion() != IngestRequest.SCHEMA_VERSION) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Ingest request schema version mismatch");
    }

    if (context.document().requestHeader().requestId() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Ingest request is missing a request identifier");
    }

    if (context.envelopeRequestId() != null
        && !context.envelopeRequestId().equals(context.document().requestHeader().requestId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Ingest envelope request identifier does not match the payload request header");
    }

    if (context.document().target() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Ingest request is missing a target reference");
    }

    if (context.document().target().targetId() == null || context.document().target().normalizedUrl() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Ingest target reference is missing canonical identity information");
    }

    if (context.document().target().depth() < 0) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Ingest target depth must be non-negative");
    }

    if (context.envelopeTargetId() != null
        && !context.envelopeTargetId().equals(context.document().target().targetId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Ingest envelope target identifier does not match the payload target reference");
    }

    if (context.document().provenance() == null || context.document().provenance().origin() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Ingest request is missing provenance information");
    }

    return new PolicyStep.Continue<>(state);
  }

  private static PolicyStep<IngestState> reject(IngestState state, PolicyReason reason, String detail) {
    return new PolicyStep.Decide<>(
        PolicyDecision.error(state.withDecision(IngestDecision.INVALID_TARGET), reason, detail));
  }
}
