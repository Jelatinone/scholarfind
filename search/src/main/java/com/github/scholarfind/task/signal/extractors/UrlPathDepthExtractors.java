package com.github.scholarfind.task.signal.extractors;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;

import com.github.scholarfind.models.Trace;
import com.github.scholarfind.models.context.ContextDocument;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalExtractionResult;
import com.github.scholarfind.task.signal.SignalExtractor;
import com.github.scholarfind.task.signal.SignalIdentifier;
import com.github.scholarfind.task.signal.SignalValue;

import lombok.NonNull;

public enum UrlPathDepthExtractors {

	STANDARD((trace, context) -> {
		int depth = (int) Arrays.stream(trace.url().getPath().split("/"))
				.filter(part -> !part.isEmpty())
				.count();

		return new SignalExtractionResult.Both(SignalCost.HEAD, Optional.of(new SignalValue.NumericSignal(depth)));
	});

	SignalExtractor _extractor;

	UrlPathDepthExtractors(final BiFunction<Trace, ContextDocument, SignalExtractionResult> rawExtractor) {
		_extractor = new SignalExtractor() {

			@Override
			public @NonNull SignalIdentifier identifier() {
				return SignalIdentifier.URL_PATH_DEPTH;
			}

			@Override
			public SignalExtractionResult extract(@NonNull Trace trace, ContextDocument context) {
				return rawExtractor.apply(trace, context);
			}

		};
	}

	public SignalExtractor extractor() {
		return _extractor;
	}
}
