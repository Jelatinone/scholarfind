package com.github.jelatinone.task.policy;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;
import com.github.jelatinone.task.signal.SignalPlanner;

public final class ClassificationPolicy implements Policy<InvestigateContext, InvestigateState> {

	@Override
	public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
		if (state.classificationResolved()) {
			return new PolicyStep.Continue<>(state);
		}

		ClassificationStub result = new SignalPlanner(
				context.classificationConfiguration(),
				context.acquisitionService())
				.classify(new AcquiredContent(
						context.document().requestHeader(),
						context.document().target(),
						context.document().trace(),
						context.retrievedContent(),
						null,
						null,
						false));
		return new PolicyStep.Continue<>(
				state.withClassification(result, result.confidence(), false));
	}
}
