package com.github.jelatinone.policy;

import java.time.Duration;

import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.shared.Request;

public record EmissionIntent<Emit extends Request>(
		Emit request,
		Duration delay,
		ProcessingStage forwardRef,
		String dedupeKey) {
}
