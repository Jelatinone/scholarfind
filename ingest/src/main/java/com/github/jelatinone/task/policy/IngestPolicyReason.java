package com.github.jelatinone.task.policy;

import com.github.jelatinone.models.shared.ReasonCode;

public enum IngestPolicyReason implements ReasonCode {
  TARGET_NOT_CANONICAL,
  TARGET_DEPTH_EXCEEDED,
  TARGET_DUPLICATE_SUPPRESSED,
  TARGET_ADMITTED;

  @Override
  public String code() {
    return name();
  }
}
