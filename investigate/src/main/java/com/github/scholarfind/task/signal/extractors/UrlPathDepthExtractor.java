package com.github.scholarfind.task.signal.extractors;

import java.util.Arrays;
import java.util.Optional;

import com.github.scholarfind.models.shared.ContentDocument;
import com.github.scholarfind.models.shared.TraceReference;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalExtractionResult;
import com.github.scholarfind.task.signal.SignalExtractor;
import com.github.scholarfind.task.signal.SignalIdentifier;
import com.github.scholarfind.task.signal.SignalValue;

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
