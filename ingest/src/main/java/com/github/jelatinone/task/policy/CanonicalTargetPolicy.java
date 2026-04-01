package com.github.jelatinone.task.policy;

import java.util.Objects;

import com.github.jelatinone.models.ingest.IngestDecision;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.IngestContext;
import com.github.jelatinone.task.IngestState;

public final class CanonicalTargetPolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    TargetReference target = context.document().target();
    if (target == null || target.normalizedUrl() == null || target.targetId() == null) {
      IngestState nextState = state.withDecision(IngestDecision.INVALID_TARGET);
      return new PolicyStep.Decide<>(
          PolicyDecision.error(nextState, IngestPolicyReason.TARGET_NOT_CANONICAL,
              "Ingest request is missing canonical target information"));
    }

    TargetReference canonical;
    try {
      canonical = TargetReference.canonical(
          target.normalizedUrl(),
          target.parentTargetId(),
          target.depth(),
          target.discoveredAt());
    } catch (IllegalArgumentException exception) {
      IngestState nextState = state.withDecision(IngestDecision.INVALID_TARGET);
      return new PolicyStep.Decide<>(
          PolicyDecision.error(nextState, IngestPolicyReason.TARGET_NOT_CANONICAL,
              "Ingest target could not be canonicalized"));
    }

    boolean canonicalTarget = target.targetId().equals(canonical.targetId())
        && Objects.equals(target.normalizedUrl().toExternalForm(), canonical.normalizedUrl().toExternalForm())
        && Objects.equals(target.parentTargetId(), canonical.parentTargetId())
        && target.depth() == canonical.depth()
        && Objects.equals(target.discoveredAt(), canonical.discoveredAt());
    if (!canonicalTarget) {
      IngestState nextState = state.withDecision(IngestDecision.INVALID_TARGET);
      return new PolicyStep.Decide<>(
          PolicyDecision.error(nextState, IngestPolicyReason.TARGET_NOT_CANONICAL,
              "Ingest request target does not match the standard canonical form"));
    }

    return new PolicyStep.Continue<>(state);
  }
}
