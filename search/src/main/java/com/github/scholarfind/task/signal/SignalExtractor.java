package com.github.scholarfind.task.signal;

import com.github.scholarfind.models.Trace;
import com.github.scholarfind.models.context.ContextDocument;

import lombok.NonNull;

public interface SignalExtractor {
	@NonNull
	SignalIdentifier identifier();

	SignalExtractionResult extract(@NonNull Trace trace, ContextDocument context);
}