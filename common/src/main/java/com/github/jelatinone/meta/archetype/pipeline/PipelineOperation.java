package com.github.jelatinone.meta.archetype.pipeline;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.policy.PolicyResult;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PipelineOperation<In extends Request, Out extends Request, Context, State, Documents extends Document<Documents>>
		implements Operate<PolicyResult<Letter<In>, Context, State>, PipelineResult<Documents, In>> {

	PipelineArchetype<In, Out, Context, State, Documents> archetype;

	@Override
	public PipelineResult<Documents, In> operate(PolicyResult<Letter<In>, Context, State> operand) {
		return archetype.processPipeline(operand);
	}
}
