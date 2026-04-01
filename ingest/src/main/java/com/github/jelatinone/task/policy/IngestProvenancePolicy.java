package com.github.jelatinone.task.policy;

import com.github.jelatinone.models.ingest.IngestDecision;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.IngestContext;
import com.github.jelatinone.task.IngestState;

public final class IngestProvenancePolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    if (context.document().provenance() == null || context.document().provenance().origin() == null) {
      return new PolicyStep.Decide<>(
          PolicyDecision.error(state.withDecision(IngestDecision.INVALID_TARGET), PolicyReason.REQUEST_REJECTED,
              "Ingest request is missing provenance information"));
    }
    return new PolicyStep.Continue<>(state);
  }
}
