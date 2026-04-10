package com.github.jelatinone.task.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.jelatinone.models.annotate.AnnotateRequest;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.models.shared.ReasonCode;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.policy.EmissionIntent;
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
		List<EmissionIntent<? extends Request>> emissions = new ArrayList<>(state.emissions());
		if (selection.minimumConfidenceExceeded()) {
			switch (selection.dominantKind()) {
				case SCHOLARSHIP, UNCLASSIFIED -> emissions.add(annotateEmission(context, state.classification()));
				case AGGREGATOR -> {
					if (emissions.isEmpty()) {
						emissions.add(annotateEmission(context, state.classification()));
					}
				}
				case LANDING, NOT_APPLICABLE -> {
					// Toss useless result :D
				}
			}
		}
		if (!emissions.isEmpty()) {
			Set<ReasonCode> reasonCodes = state.reusedRecentClassification()
					? Set.of(PolicyReason.RECENT_RESULT_REUSED)
					: Set.of();
			return new PolicyStep.Decide<>(
					PolicyDecision.next(
							state,
							reasonCodes,
							"Investigate request forwarded to downstream stages",
							List.copyOf(emissions)));
		}
		String reasonDetail = !selection.hasSignals()
				? "Investigate classification produced no signals"
				: selection.minimumConfidenceExceeded()
						? "Investigate request produced no downstream follow-up"
						: "Investigate request did not satisfy forwarding gate";
		return new PolicyStep.Decide<>(
				PolicyDecision.drop(
						state,
						PolicyReason.REQUEST_REJECTED,
						reasonDetail));
	}

	private EmissionIntent<AnnotateRequest> annotateEmission(
			InvestigateContext context,
			ClassificationStub classification) {
		AnnotateRequest request = new AnnotateRequest(
				context.document().requestHeader(),
				context.document().target(),
				classification);
		return new EmissionIntent<>(
				request,
				null,
				ProcessingStage.ANNOTATE,
				context.document().requestHeader().idempotencyKey());
	}
}
