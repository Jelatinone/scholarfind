package com.github.jelatinone.policy.base;

import java.time.Duration;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyContext;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ExpirationPolicy<Requests extends Request<Requests>, Documents extends Document<Documents>, Context extends PolicyContext<Requests, Documents>, State>
    implements Policy<Context, State> {
  Duration expirationDays;

  @Override
  public PolicyStep<State> apply(Context context, State state) {
    return context.retrievedDocument()
        .map(document -> document.documentHeader().emittedAt())
        .map(discoveredAt -> discoveredAt.plus(expirationDays))
        .filter(expiresAt -> expiresAt.isBefore(context.envelopeReviewedAt()))
        .<PolicyStep<State>>map(expiresAt -> new PolicyStep.Decide<>(
            new PolicyDecision.Drop<>(
                state,
                PolicyReason.DOCUMENT_EXPIRED,
                "Target expired before stage processing")))
        .orElseGet(() -> new PolicyStep.Continue<>(state));
  }
}
