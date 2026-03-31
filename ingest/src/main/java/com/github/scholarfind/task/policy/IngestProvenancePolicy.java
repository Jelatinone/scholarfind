package com.github.scholarfind.task.policy;

import com.github.scholarfind.models.ingest.IngestDecision;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyReason;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.IngestContext;
import com.github.scholarfind.task.IngestState;

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
