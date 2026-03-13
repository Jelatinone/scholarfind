package com.github.scholarfind.models.investigate;

public enum ClassificationType {
  UNCLASSIFIED,
  NOT_APPLICABLE,
  LANDING,
  AGGREGATOR,
  SCHOLARSHIP;

  public static ClassificationType max(ClassificationType first, ClassificationType second) {
    if (first.ordinal() > second.ordinal()) {
      return first;
    }
    return second;
  }
}
