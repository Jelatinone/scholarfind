package com.github.scholarfind.validation;

import com.github.scholarfind.models.shared.ReasonCode;

public enum Reason implements ReasonCode {
  SCHEMA_MISMATCH,

  DOCUMENT_EXPIRED,

  ATTEMPTS_EXCEEDED,

  DOCUMENT_ERROR,

  REQUEST_REJECTED,

  REQUEST_DEAD_LETTERED;

  @Override
  public String code() {
    return name();
  }
}
