package com.github.jelatinone.model.transit;

import java.time.Duration;

import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public record Emission<Emit extends Request<Emit>>(
		@NonNull Emit request,
		@NonNull Duration delay,
		@NonNull ExecutionStage executionRef) {
}
