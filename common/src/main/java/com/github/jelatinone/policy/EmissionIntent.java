package com.github.jelatinone.policy;

import java.time.Duration;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public record EmissionIntent<Emit extends Request>(
		@NonNull Emit request,
		@NonNull Duration delay,
		@NonNull ExecutionStage executionRef) {
}
