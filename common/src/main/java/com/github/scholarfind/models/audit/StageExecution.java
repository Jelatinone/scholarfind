package com.github.scholarfind.models.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.scholarfind.models.shared.ReasonCode;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.StageOutcome;

public record StageExecution(
    long schemaVersion,
    UUID targetId,
    ProcessingStage stage,
    int attemptCount,
    StageOutcome lastOutcome,
    String lastReasonCode,
    String lastReasonDetail,
    Instant nextAttemptAt,
    Instant updatedAt,
    List<StageTransition> transitions) {

  public static final long SCHEMA_VERSION = 1L;

  public static StageExecution initial(UUID targetId, ProcessingStage stage, Instant now) {
    return new StageExecution(
        SCHEMA_VERSION,
        targetId,
        stage,
        0,
        null,
        null,
        null,
        null,
        now,
        List.of());
  }

  public static String key(UUID targetId, ProcessingStage stage) {
    return String.format("%s:%s", targetId, stage);
  }

  public StageExecution transition(
      PolicyDecision<?> decision,
      Instant occurredAt,
      Instant nextAttemptAt,
      int maxTransitions,
      boolean incrementAttempt) {
    List<StageTransition> nextTransitions = new ArrayList<>(transitions);
    nextTransitions.add(new StageTransition(
        occurredAt,
        decision.outcome(),
        primaryCode(decision.primaryReasonCode()),
        decision.reasonDetail()));

    if (nextTransitions.size() > maxTransitions) {
      nextTransitions = nextTransitions.subList(nextTransitions.size() - maxTransitions, nextTransitions.size());
    }

    return new StageExecution(
        schemaVersion,
        targetId,
        stage,
        incrementAttempt ? attemptCount + 1 : attemptCount,
        decision.outcome(),
        primaryCode(decision.primaryReasonCode()),
        decision.reasonDetail(),
        nextAttemptAt,
        occurredAt,
        List.copyOf(nextTransitions));
  }

  private static String primaryCode(ReasonCode reasonCode) {
    return reasonCode == null ? null : reasonCode.code();
  }
}
