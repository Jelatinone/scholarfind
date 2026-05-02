package com.github.jelatinone.utility.scheduler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConstantBackoffScheduler implements BackoffScheduler {

	Long _baseBackoff;

	@Override
	public long compute() {
		return _baseBackoff;
	}

	@Override
	public long compute(int step) {
		return _baseBackoff;
	}

	@Override
	public long reset() {
		return _baseBackoff;
	}

	@Override
	public long acquire() {
		return _baseBackoff;
	}

}
