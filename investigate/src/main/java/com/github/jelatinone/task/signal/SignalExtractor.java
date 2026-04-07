package com.github.jelatinone.task.signal;

import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.models.investigate.ClassificationStub;

import lombok.NonNull;

public interface SignalExtractor {

  @NonNull
  SignalTier requires();

  @NonNull
  SignalIdentity identity();

  boolean supports(@NonNull AcquiredContent acquisition);

  double cost(@NonNull AcquiredContent acquisition, @NonNull ClassificationStub stub, @NonNull SignalPattern pattern);

  Optional<SignalValue> extract(@NonNull AcquiredContent acquisition);

  public static double metadataCost(AcquiredContent acquisition, SignalPattern pattern) {
    if (acquisition.hasMetadata()) {
      return pattern.positiveMetadataCost();
    }
    return pattern.negativeMetadataCost();
  }

  public static double contentCost(AcquiredContent acquisition, SignalPattern pattern) {
    if (acquisition.hasHydratedSource()) {
      return pattern.hydratedContentCost();
    }
    if (acquisition.hasSourceSnapshot()) {
      return pattern.snapshotContentCost();
    }
    return pattern.negativeContentCost();
  }

  public static double textCost(AcquiredContent acquisition, SignalPattern pattern) {
    if (acquisition.hasCompleteText()) {
      return pattern.completeTextCost();
    }
    if (acquisition.hasTextSnapshot()) {
      return pattern.snapshotTextCost();
    }
    if (acquisition.hasSourceSnapshot() || acquisition.hasHydratedSource()) {
      return pattern.hydratedTextCost();
    }
    return pattern.negativeTextCost();
  }
}
