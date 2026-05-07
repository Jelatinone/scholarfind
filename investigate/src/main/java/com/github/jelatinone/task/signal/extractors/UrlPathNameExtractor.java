package com.github.jelatinone.task.signal.extractors;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalPattern;
import com.github.jelatinone.task.signal.SignalTier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.NonNull;

public final class UrlPathNameExtractor implements SignalExtractor {

  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.TRACE;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.URL_PATH_NAME;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent context) {
    return context.traceUrl() != null;
  }

  @Override
  public double cost(@NonNull AcquiredContent context, @NonNull Classification.Collected stub,
      @NonNull SignalPattern pattern) {
    return 0D;
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent context) {
    URL url = context.traceUrl();
    List<String> parts;
    if (url == null || url.getPath() == null || url.getPath().isBlank()) {
      parts = List.of();
    } else {
      parts = Arrays.stream(url.getPath().split("/"))
        .map(String::trim)
        .filter(segment -> !segment.isBlank())
        .toList();
    }
    return Optional.of(new SignalValue.ListSignal<>(parts));
  }
}
