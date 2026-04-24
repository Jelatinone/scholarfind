package com.github.jelatinone.meta.archetype.policy;

import java.time.Instant;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.result.OperationResult;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyPipeline;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyOperation<Input, Context, State, Document>
		implements Operate<Input, PolicyResult<Input, Context, State, Document>> {

	PolicyContextBuilder<Input, Context> contextBuilder;
	PolicyStateBuilder<Context, State> stateBuilder;
	PolicyDocumentBuilder<Input, Context, State, Document> documentBuilder;

	PolicyPipeline<Context, State> policyPipeline;

	@Override
	public OperationResult<PolicyResult<Input, Context, State, Document>> operate(Input operand) {
		Instant startedAt = Instant.now();

		Context context = contextBuilder.build(operand, startedAt);
		State state = stateBuilder.build(context);
		PolicyDecision<State> decision = policyPipeline.process(context, state);

		Instant occurredAt = Instant.now();
		Document document = documentBuilder.build(operand, context, decision, occurredAt);

		return new OperationResult<>(new PolicyResult<>(
				operand,
				context,
				decision,
				document,
				startedAt,
				occurredAt));
	}
}
