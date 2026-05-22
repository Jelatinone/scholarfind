package com.github.jelatinone.policy.base;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyContext;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.policy.PolicyRejecter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RequestSchemaPolicy<Requests extends Request<Requests>, Documents extends Document<Documents>, Context extends PolicyContext<Requests, Documents>, State>
    implements Policy<Context, State> {

  long expectedSchemaVersion;
  PolicyRejecter<State> rejecter;

  @Override
  public PolicyStep<State> apply(Context context, State state) {
    return context.retrievedDocument()
        .map(document -> validate(context, state, document))
        .orElseGet(() -> new PolicyStep.Continue<>(state));
  }

  private PolicyStep<State> validate(Context context, State state, Documents document) {
    if (context.envelopeSchemaVersion() != RequestHeader.SCHEMA_VERSION) {
      return reject(state, PolicyReason.SCHEMA_MISMATCH, "Envelope schema version mismatch");
    }

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
