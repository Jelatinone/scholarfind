package com.github.scholarfind.policy;

import com.github.scholarfind.models.shared.ReasonCode;

public enum PolicyReason implements ReasonCode {
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
