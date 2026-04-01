package com.github.jelatinone.task.signal.extractors;

import java.util.Arrays;
import java.util.Optional;

import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.shared.TraceReference;
import com.github.jelatinone.task.signal.SignalCost;
import com.github.jelatinone.task.signal.SignalExtractionResult;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentifier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.NonNull;

public class UrlPathDepthExtractor implements SignalExtractor {

  @Override
  public @NonNull SignalIdentifier identifier() {
    return SignalIdentifier.URL_PATH_DEPTH;
  }

  @Override
  public SignalExtractionResult extract(@NonNull TraceReference trace, ContentDocument content) {
    int depth = (int) Arrays.stream(trace.normalizedUrl().getPath().split("/"))
        .filter(part -> !part.isEmpty())
        .count();

    return new SignalExtractionResult.Both(SignalCost.HEAD, Optional.of(new SignalValue.NumericSignal(depth)));
  }

}
