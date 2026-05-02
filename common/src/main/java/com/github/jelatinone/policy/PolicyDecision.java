package com.github.jelatinone.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jelatinone.model.audit.AttemptReason;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Emission;

public record PolicyDecision<State>(
    State state,
    StageOutcome outcome,

    Set<AttemptReason> reasons,
    String details,

    List<Emission<? extends Request>> emissions,
    Map<String, String> attributes) {

  public PolicyDecision {
    reasons = Set.copyOf(reasons);
    emissions = List.copyOf(emissions);
    attributes = Map.copyOf(attributes);
  }

  public static <State> PolicyDecision<State> next(State state) {
    return new PolicyDecision<>(state, StageOutcome.NEXT, Set.of(), null, List.of(), Map.of());
  }

  public static <State> PolicyDecision<State> next(
      State state,
      Set<AttemptReason> reasons,
      String detail,
      List<Emission<? extends Request>> emissions) {
    return new PolicyDecision<>(state, StageOutcome.NEXT, reasons, detail, emissions, Map.of());
  }

  public static <State> PolicyDecision<State> drop(
      State state,
      AttemptReason reason,
      String detail) {
    return new PolicyDecision<>(state, StageOutcome.DROP, Set.of(reason), detail, List.of(), Map.of());
  }

  public static <State> PolicyDecision<State> retry(
      State state,
      AttemptReason reason,
      String detail) {
    return new PolicyDecision<>(state, StageOutcome.RETRY, Set.of(reason), detail, List.of(),
        Map.of());
  }

  public static <State> PolicyDecision<State> error(
      State state,
      AttemptReason reason,
      String detail) {
    return new PolicyDecision<>(state, StageOutcome.ERROR, Set.of(reason), detail, List.of(), Map.of());
  }
}
