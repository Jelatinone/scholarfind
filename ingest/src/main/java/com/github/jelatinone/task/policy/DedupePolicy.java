package com.github.jelatinone.task.policy;

import java.time.Instant;

import com.github.jelatinone.models.ingest.IngestDecision;
import com.github.jelatinone.models.ingest.TargetRecord;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.IngestContext;
import com.github.jelatinone.task.IngestState;

public final class DedupePolicy implements Policy<IngestContext, IngestState> {

  @Override
  public PolicyStep<IngestState> apply(IngestContext context, IngestState state) {
    TargetRecord targetRecord = context.targetRecord();
    if (targetRecord == null || targetRecord.lastScheduledAt() == null) {
      return new PolicyStep.Continue<>(state);
    }

    Instant cutoff = context.reviewedAt().minus(context.rescheduleCooldown());
    if (targetRecord.lastScheduledAt().isBefore(cutoff)) {
      return new PolicyStep.Continue<>(state);
    }

    IngestState nextState = state.withDecision(IngestDecision.DUPLICATE_SUPPRESSED);
    return new PolicyStep.Decide<>(
        PolicyDecision.drop(nextState, IngestPolicyReason.TARGET_DUPLICATE_SUPPRESSED,
            "Ingest target was recently scheduled and remains inside the reschedule cooldown"));
  }
}
