package com.github.jelatinone.models.shared;

public enum RequestState {
  QUEUED,
  PROCESSING,
  FORWARDED,
  RETRY_SCHEDULED,
  REJECTED,
  DEAD_LETTERED
}
