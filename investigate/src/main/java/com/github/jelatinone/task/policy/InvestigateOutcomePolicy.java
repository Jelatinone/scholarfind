package com.github.jelatinone.task.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.jelatinone.model.annotate.AnnotateRequest;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;

public final class InvestigateOutcomePolicy implements Policy<InvestigateContext, InvestigateState> {
  private final ClassificationConfiguration configuration;

  public InvestigateOutcomePolicy(ClassificationConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
    ClassificationSelection selection = ClassificationSelection.resolve(state.classification(), configuration);
    List<Emission<? extends Request>> emissions = new ArrayList<>(state.emissions());
    if (selection.minimumConfidenceExceeded()) {
      switch (selection.dominantKind()) {
        case ARCHIVE, UNCLASSIFIED -> emissions.add(annotateEmission(context, state.classification()));
        case AGGREGATOR -> {
          if (emissions.isEmpty()) {
            emissions.add(annotateEmission(context, state.classification()));
          }
        }
        case LANDING, SEARCH -> {
        }
      }
    }
    if (!emissions.isEmpty()) {
      PolicyReason reason = state.reusedRecentClassification()
          ? PolicyReason.RECENT_RESULT_REUSED
          : PolicyReason.OPERATION_CONTINUITY;
      return new PolicyStep.Decide<>(
          new PolicyDecision.Next<>(
              state.withEmissions(List.copyOf(emissions)),
              reason,
              "Investigate request forwarded to downstream stages",
              Set.copyOf(emissions)));
    }
    String reasonDetail = !selection.hasSignals()
        ? "Investigate classification produced no signals"
        : selection.minimumConfidenceExceeded()
            ? "Investigate request produced no downstream follow-up"
            : "Investigate request did not satisfy forwarding gate";
    return new PolicyStep.Decide<>(
        new PolicyDecision.Drop<>(
            state,
            PolicyReason.REQUEST_REJECTED,
            reasonDetail));
  }

  private Emission<AnnotateRequest> annotateEmission(
      InvestigateContext context,
      Classification.Collected classification) {
    RequestHeader current = context.document().requestHeader();
    RequestHeader header = new RequestHeader(
        current.schemaVersion(),
        context.envelopeTargetId(),
        context.envelopeReviewId(),
        0,
        ExecutionStage.INVESTIGATE,
        Instant.now());
    AnnotateRequest request = new AnnotateRequest(
        header,
        context.envelopeTargetId(),
        context.envelopeReviewId(),
        classification);
    return new Emission<>(
        request,
        Duration.ZERO,
        ExecutionStage.ANNOTATE);
  }
}
