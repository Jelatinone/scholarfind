package com.github.jelatinone.meta.archetype.pipeline;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.policy.PolicyResult;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PipelineOperation<In extends Request<In>, Out extends Request<Out>, Context, State, Documents extends Document<Documents>>
		implements Operate<PolicyResult<In, Context, State>, PipelineResult<Documents, In>> {

	PipelineArchetype<In, Out, Context, State, Documents> archetype;

	@Override
	public PipelineResult<Documents, In> operate(PolicyResult<In, Context, State> operand) {
		return archetype.processPipeline(operand);
	}
}
