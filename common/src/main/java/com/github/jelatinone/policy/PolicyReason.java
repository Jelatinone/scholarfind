package com.github.jelatinone.policy;

import com.github.jelatinone.model.audit.AttemptReason;

public enum PolicyReason implements AttemptReason {
	OPERATION_CONTINUITY,
	SCHEMA_MISMATCH,
	DOCUMENT_EXPIRED,
	ATTEMPTS_EXCEEDED,
	REQUEST_REJECTED,
	RECENT_RESULT_REUSED,
	OPERATION_EXCEPTION;

	@Override
	public String reason() {
		return name();
	}
}
