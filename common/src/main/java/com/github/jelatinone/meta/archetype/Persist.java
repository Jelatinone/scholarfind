package com.github.jelatinone.meta.archetype;

import com.github.jelatinone.meta.result.PostResult;

import lombok.NonNull;

@FunctionalInterface
public interface Persist<Produces> {

	/**
	 * Self-callback function to determine the validity of the resulting data
	 * 
	 * @param operand Data to be checked
	 * @return Mapped result
	 */
	@NonNull
	PostResult post(Produces operand);
}
