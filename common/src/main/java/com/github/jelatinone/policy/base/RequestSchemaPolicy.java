package com.github.jelatinone.policy.base;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.policy.Rejecter;
import com.github.jelatinone.policy.RequestPolicyContext;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RequestSchemaPolicy<Doc extends Document<Doc>, Context extends RequestPolicyContext<Doc>, State>
    implements Policy<Context, State> {

  long expectedSchemaVersion;
  ExecutionStage expectedExecutionStage;

  Rejecter<State> rejecter;

  @Override
  public PolicyStep<State> apply(Context context, State state) {
    if (context.envelopeSchemaVersion() != Letter.SCHEMA_VERSION) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Envelope schema version mismatch");
    }

    if (context.envelopeStage() != expectedExecutionStage) {
      return reject(state, PolicyReason.REQUEST_REJECTED, "Envelope stage does not match the expected pipeline stage");
    }

    Doc document = context.document();

    if (document.requestHeader().schemaVersion() != expectedSchemaVersion) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Request schema version mismatch");
    }

    if (!document.targetId().equals(document.requestHeader().targetId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Payload target identifier does not match the request header");
    }

    if (!context.envelopeTargetId().equals(document.targetId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Envelope target identifier does not match the payload document");
    }

    if (!context.envelopeReviewId().equals(document.reviewId())) {
      return reject(state, PolicyReason.REQUEST_REJECTED,
          "Envelope review identifier does not match the payload document");
    }

    return new PolicyStep.Continue<>(state);
  }

  private PolicyStep<State> reject(State state, PolicyReason reason, String detail) {
    return new PolicyStep.Decide<>(rejecter.reject(state, reason, detail));
  }
}
