package com.github.jelatinone.task.signal.extractors;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalPattern;
import com.github.jelatinone.task.signal.SignalTier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.NonNull;

public final class AnchorDensityExtractor implements SignalExtractor {
	@Override
	public @NonNull SignalTier requires() {
		return SignalTier.CONTENT;
	}

	@Override
	public @NonNull SignalIdentity identity() {
		return SignalIdentity.ANCHOR_DENSITY;
	}

	@Override
	public boolean supports(@NonNull AcquiredContent acquisition) {
		return !acquisition.hasMetadata() || isHTMLCapable(acquisition);
	}

	@Override
	public double cost(@NonNull AcquiredContent acquisition, @NonNull Classification.Collected stub,
			@NonNull SignalPattern pattern) {
		return SignalExtractor.contentCost(acquisition, pattern);
	}

	@Override
	public Optional<SignalValue> extract(@NonNull AcquiredContent acquisition) {
		if (!isHTMLCapable(acquisition)) {
			return Optional.empty();
		}
		return acquisition.decodedSource()
				.map(source -> {
					Document document = Jsoup.parse(
							source,
							acquisition.effectiveUrl() == null
									? ""
									: acquisition.effectiveUrl().toString());
					int anchorCount = document.select("a").size();
					int wordCount = Math.max(1, document.text().trim().split("\\s+").length);
					return new SignalValue.NumericSignal((double) anchorCount / wordCount);
				});
	}

	public static boolean isHTMLCapable(@NonNull AcquiredContent acquisition) {
		return acquisition.mediaType() == MediaType.TEXT_HTML;
	}
}
