package com.github.scholarfind.task.signal;

import com.github.scholarfind.models.shared.ContextDocument;
import com.github.scholarfind.models.shared.TraceReference;

import lombok.NonNull;

public interface SignalExtractor {
  @NonNull
  SignalIdentifier identifier();

  SignalExtractionResult extract(@NonNull TraceReference trace, ContextDocument context);
}
