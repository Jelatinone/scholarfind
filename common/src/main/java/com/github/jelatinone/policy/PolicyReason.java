package com.github.jelatinone.policy;

import com.github.jelatinone.models.shared.ReasonCode;

public enum PolicyReason implements ReasonCode {
	OPERATION_CONTINUITY,
	SCHEMA_MISMATCH,
	DOCUMENT_EXPIRED,
	ATTEMPTS_EXCEEDED,
	REQUEST_REJECTED,
	RECENT_RESULT_REUSED,
	OPERATION_EXCEPTION;

	@Override
	public String code() {
		return name();
	}
}
