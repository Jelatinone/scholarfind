package com.github.jelatinone.task.signal.extractors;

import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalIdentity;
import com.github.jelatinone.task.signal.SignalPattern;
import com.github.jelatinone.task.signal.SignalTier;
import com.github.jelatinone.task.signal.SignalValue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public final class TextWordCountExtractor implements SignalExtractor {

  @Override
  public @NonNull SignalTier requires() {
    return SignalTier.TEXT;
  }

  @Override
  public @NonNull SignalIdentity identity() {
    return SignalIdentity.TEXT_WORD_COUNT;
  }

  @Override
  public boolean supports(@NonNull AcquiredContent context) {
    MediaType mediaType = context.mediaType();
    return mediaType == null
        || mediaType == MediaType.TEXT_HTML
        || mediaType == MediaType.APPLICATION_PDF
        || mediaType == MediaType.TEXT_PLAIN
        || mediaType == MediaType.TEXT_MARKDOWN
        || mediaType == MediaType.APPLICATION_XML
        || mediaType == MediaType.TEXT_XML
        || mediaType == MediaType.APPLICATION_JSON
        || mediaType == MediaType.TEXT_JSON;
  }

  @Override
  public double cost(
      @NonNull AcquiredContent context,
      @NonNull Classification.Collected stub,
      @NonNull SignalPattern pattern) {
    return SignalExtractor.textCost(context, pattern);
  }

  @Override
  public Optional<SignalValue> extract(@NonNull AcquiredContent context) {
    if (!context.hasCompleteText()) {
      return Optional.empty();
    }
    String normalizedText = context.hydratedText();
    int wordCount = normalizedText == null || normalizedText.isBlank()
        ? 0
        : normalizedText.trim().split("\\s+").length;
    return Optional.of(new SignalValue.NumericSignal(wordCount));
  }
}
