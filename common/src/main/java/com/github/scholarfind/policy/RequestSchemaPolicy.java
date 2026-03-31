package com.github.scholarfind.policy;

import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.StageEnvelope;

public final class RequestSchemaPolicy<D extends StageDocument<D>, C extends RequestPolicyContext<D>, State>
    implements Policy<C, State> {

  private final ProcessingStage expectedStage;
  private final long expectedRequestSchemaVersion;
  private final Rejecter<State> rejecter;

  public RequestSchemaPolicy(
      ProcessingStage expectedStage,
      long expectedRequestSchemaVersion,
      Rejecter<State> rejecter) {
    this.expectedStage = expectedStage;
    this.expectedRequestSchemaVersion = expectedRequestSchemaVersion;
    this.rejecter = rejecter;
  }

  @Override
  public PolicyStep<State> apply(C context, State state) {
    if (context.envelopeSchemaVersion() != StageEnvelope.SCHEMA_VERSION) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Envelope schema version mismatch");
    }

    if (context.envelopeStage() != expectedStage) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Envelope stage does not match the expected pipeline stage");
    }

    D document = context.document();
    if (document == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Pipeline request did not materialize a stage document");
    }

    if (document.requestHeader() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Pipeline request is missing a request header");
    }

    if (document.requestHeader().schemaVersion() != expectedRequestSchemaVersion) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Request schema version mismatch");
    }

    if (document.requestHeader().requestId() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Pipeline request is missing a request identifier");
    }

    if (context.envelopeRequestId() != null
        && !context.envelopeRequestId().equals(document.requestHeader().requestId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Envelope request identifier does not match the payload request header");
    }

    if (document.target() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Pipeline request is missing a target reference");
    }

    if (document.target().targetId() == null || document.target().normalizedUrl() == null) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Pipeline target reference is missing canonical identity information");
    }

    if (document.target().depth() < 0) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Pipeline target depth must be non-negative");
    }

    if (context.envelopeTargetId() != null
        && !context.envelopeTargetId().equals(document.target().targetId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Envelope target identifier does not match the payload target reference");
    }

    return new PolicyStep.Continue<>(state);
  }

  private PolicyStep<State> reject(State state, PolicyReason reason, String detail) {
    return new PolicyStep.Decide<>(rejecter.reject(state, reason, detail));
  }
}
