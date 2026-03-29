package com.github.scholarfind.task.policy;

import java.util.List;
import java.util.Set;

import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.policy.EmissionIntent;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.IngestContext;
import com.github.scholarfind.task.IngestState;
import com.github.scholarfind.models.ingest.IngestDecision;

public final class IngestOutcomePolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    IngestState nextState = state.withDecision(IngestDecision.ADMITTED);
    InvestigateRequest request = new InvestigateRequest(
        context.document().requestHeader(),
        context.document().target());

    return new PolicyStep.Decide<>(
        PolicyDecision.next(
            nextState,
            Set.of(IngestPolicyReason.TARGET_ADMITTED),
            "Ingest target admitted for investigation",
            List.of(new EmissionIntent<>(
                request,
                null,
                null,
                context.document().requestHeader().idempotencyKey()))));
  }
}
