package com.github.scholarfind.task.signal;

public enum SignalCost {
	FREE,
	HEAD,
	SHALLOW,
	FULL;

	public static SignalCost max(SignalCost first, SignalCost second) {
		if (first.ordinal() > second.ordinal()) {
			return first;
		}
		return second;
	}
}
