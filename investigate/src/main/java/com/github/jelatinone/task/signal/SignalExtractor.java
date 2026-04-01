package com.github.jelatinone.task.signal;

import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.shared.TraceReference;

import lombok.NonNull;

public interface SignalExtractor {
  @NonNull
  SignalIdentifier identifier();

  SignalExtractionResult extract(@NonNull TraceReference trace, ContentDocument content);
}
