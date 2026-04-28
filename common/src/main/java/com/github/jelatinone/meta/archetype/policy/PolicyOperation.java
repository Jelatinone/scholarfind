package com.github.jelatinone.meta.archetype.policy;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyOperation<In extends Request, Context, State, D extends Document<D>>
		implements Operate<Letter<In>, PolicyResult<Letter<In>, Context, State, D>> {

	PolicyArchetype<In, Context, State, D> archetype;

	@Override
	public PolicyResult<Letter<In>, Context, State, D> operate(Letter<In> operand) {
		return archetype.processPolicy(operand);
	}
}
