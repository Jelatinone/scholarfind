package com.github.jelatinone.models.investigate;

public enum ClassificationKind {
  UNCLASSIFIED,
  NOT_APPLICABLE,
  LANDING,
  AGGREGATOR,
  SCHOLARSHIP;

  public static ClassificationKind max(ClassificationKind first, ClassificationKind second) {
    if (first.ordinal() > second.ordinal()) {
      return first;
    }
    return second;
  }
}
