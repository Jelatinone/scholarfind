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

public final class SubdomainDepthExtractor implements SignalExtractor {
  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.TRACE;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.SUBDOMAIN_DEPTH;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent context) {
    return context.traceHost() != null && !context.traceHost().isBlank();
  }

  @Override
  public double cost(@NonNull AcquiredContent context, @NonNull Classification.Collected stub,
      @NonNull SignalPattern pattern) {
    return 0D;
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent context) {
    String host = context.traceHost();
    if (host == null || host.isBlank()) {
      return Optional.empty();
    }
    String[] parts = host.split("\\.");
    return Optional.of(new SignalValue.NumericSignal(Math.max(0, parts.length - 2)));
  }
}
