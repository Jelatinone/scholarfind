package com.github.jelatinone.meta.archetype.policy;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.model.struct.Request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PolicyOperation<In extends Request<In>, Context, State>
		implements Operate<In, PolicyResult<In, Context, State>> {

	PolicyArchetype<In, Context, State> archetype;

	@Override
	public PolicyResult<In, Context, State> operate(In operand) {
		return archetype.processPolicy(operand);
	}
}
