package com.github.scholarfind.task.signal;

import java.util.Map;

public record SignalCostConfiguration(
		Map<SignalIdentifier, SignalCost> costs,
		SignalCost initial) {
	public SignalCost cost(SignalIdentifier identifier) {
		return costs.getOrDefault(identifier, SignalCost.FULL);
	}
}
