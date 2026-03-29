package com.github.scholarfind.task.policy;

import java.time.Instant;

import com.github.scholarfind.models.ingest.IngestDecision;
import com.github.scholarfind.models.ingest.IngestDocument;
import com.github.scholarfind.policy.Policy;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyStep;
import com.github.scholarfind.task.IngestContext;
import com.github.scholarfind.task.IngestState;

public final class DedupePolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    IngestDocument retrieved = context.retrievedIngest();
    if (retrieved == null || retrieved.documentHeader() == null || retrieved.documentHeader().createdAt() == null) {
      return new PolicyStep.Continue<>(state);
    }

    Instant cutoff = context.reviewedAt().minus(context.rescheduleCooldown());
    if (retrieved.documentHeader().createdAt().isBefore(cutoff)) {
      return new PolicyStep.Continue<>(state);
    }

    IngestState nextState = state.withDecision(IngestDecision.DUPLICATE_SUPPRESSED);
    return new PolicyStep.Decide<>(
        PolicyDecision.drop(nextState, IngestPolicyReason.TARGET_DUPLICATE_SUPPRESSED,
            "Ingest target was recently admitted and remains inside the reschedule cooldown"));
  }
}
