package com.github.jelatinone.task.signal.extractors;

import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalPattern;
import com.github.jelatinone.task.signal.SignalTier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.NonNull;

public final class RedirectHopsExtractor implements SignalExtractor {

  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.METADATA;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.REDIRECT_HOPS;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent acquisition) {
    return true;
  }

  @Override
  public double cost(@NonNull AcquiredContent acquisition, @NonNull ClassificationStub stub,
      @NonNull SignalPattern pattern) {
    return SignalExtractor.metadataCost(acquisition, pattern);
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent acquisition) {
    Integer value = acquisition.redirectHopCount();
    return value == null
        ? Optional.empty()
        : Optional.of(new SignalValue.NumericSignal(value));
  }
}
