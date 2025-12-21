package com.github.scholarfind.models;

public enum DecisionType {

  NEED_ANNOTATE,

  NEED_SEARCH,

  NEED_DROP,

  DATA_DUPLICATE_ENTRY,

  DATA_OUT_OF_DATE,

  ERROR_MALFORMED,

  ERROR_MUST_RETRY,

  ERROR_MUST_DROP,

  ERROR_MAX_ATTEMPTS,
}
