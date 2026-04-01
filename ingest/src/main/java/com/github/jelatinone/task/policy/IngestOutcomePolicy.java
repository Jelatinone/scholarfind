package com.github.jelatinone.task.policy;

import java.util.List;
import java.util.Set;

import com.github.jelatinone.models.ingest.IngestDecision;
import com.github.jelatinone.models.investigate.InvestigateRequest;
import com.github.jelatinone.policy.EmissionIntent;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.IngestContext;
import com.github.jelatinone.task.IngestState;

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
