package com.github.jelatinone.meta.archetype.pipeline;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PipelineOperation<In extends Request, Out extends Request, Context, State, D extends Document<D>>
		implements Operate<Letter<In>, PipelineResult<D, In>> {

	PipelineArchetype<In, Out, Context, State, D> archetype;

	@Override
	public PipelineResult<D, In> operate(Letter<In> operand) {
		return archetype.processPipeline(operand);
	}
}
