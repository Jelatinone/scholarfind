package com.github.jelatinone.task.signal.extractors;

import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalPattern;
import com.github.jelatinone.task.signal.SignalTier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.NonNull;

public final class StatusCodeExtractor implements SignalExtractor {

  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.METADATA;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.STATUS_CODE;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent context) {
    return true;
  }

  @Override
  public double cost(@NonNull AcquiredContent context, @NonNull Classification.Collected stub,
      @NonNull SignalPattern pattern) {
    return SignalExtractor.metadataCost(context, pattern);
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent context) {
    Integer value = context.statusCode();
    return value == null
        ? Optional.empty()
        : Optional.of(new SignalValue.NumericSignal(value));
  }
}
