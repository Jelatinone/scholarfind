package com.github.scholarfind.task.policy;

import com.github.scholarfind.models.ingest.IngestDecision;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.IngestContext;
import com.github.scholarfind.task.IngestState;

public final class DepthBudgetPolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    int remainingBudget = Math.max(0, context.maxDepth() - context.document().target().depth());
    IngestState nextState = state.withDepthBudget(remainingBudget);
    if (context.document().target().depth() > context.maxDepth()) {
      return new PolicyStep.Decide<>(
          PolicyDecision.drop(nextState.withDecision(IngestDecision.DEPTH_EXCEEDED),
              IngestPolicyReason.TARGET_DEPTH_EXCEEDED,
              "Ingest target exceeded the configured depth budget"));
    }

    return new PolicyStep.Continue<>(nextState);
  }
}
