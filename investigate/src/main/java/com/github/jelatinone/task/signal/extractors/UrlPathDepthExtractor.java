package com.github.jelatinone.task.signal.extractors;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalPattern;
import com.github.jelatinone.task.signal.SignalTier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.NonNull;

public final class UrlPathDepthExtractor implements SignalExtractor {

  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.TRACE;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.URL_PATH_DEPTH;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent context) {
    return context.traceUrl() != null;
  }

  @Override
  public double cost(@NonNull AcquiredContent context, @NonNull ClassificationStub stub,
      @NonNull SignalPattern pattern) {
    return 0D;
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent context) {
    URL url = context.traceUrl();
    List<String> parts;
    if (url == null || url.getPath() == null || url.getPath().isBlank()) {
      parts = List.of();
    }
    parts = Arrays.stream(url.getPath().split("/"))
        .map(String::trim)
        .filter(segment -> !segment.isBlank())
        .toList();
    return Optional.of(new SignalValue.NumericSignal(parts.size()));
  }
}
