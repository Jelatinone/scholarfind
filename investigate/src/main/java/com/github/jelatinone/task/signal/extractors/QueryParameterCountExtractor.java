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

public final class QueryParameterCountExtractor implements SignalExtractor {
  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.TRACE;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.QUERY_PARAMETER;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent acquisition) {
    return acquisition.traceUrl() != null;
  }

  @Override
  public double cost(@NonNull AcquiredContent acquisition, @NonNull ClassificationStub stub,
      @NonNull SignalPattern pattern) {
    return 0D;
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent acquisition) {
    String query = acquisition.traceQuery();
    if (query == null || query.isBlank()) {
      return Optional.of(new SignalValue.NumericSignal(0D));
    }
    return Optional.of(new SignalValue.NumericSignal(query.split("&").length));
  }
}
