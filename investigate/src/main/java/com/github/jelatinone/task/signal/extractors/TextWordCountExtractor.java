package com.github.jelatinone.task.signal.extractors;

import java.util.Optional;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.models.content.ContentKind;
import com.github.jelatinone.models.investigate.ClassificationStub;
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
    ContentKind contentKind = context.contentKind();
    return contentKind == null
        || contentKind == ContentKind.HTML
        || contentKind == ContentKind.PDF
        || contentKind == ContentKind.PLAIN_TEXT
        || contentKind == ContentKind.MARKDOWN
        || contentKind == ContentKind.XML
        || contentKind == ContentKind.JSON;
  }

  @Override
  public double cost(
      @NonNull AcquiredContent context,
      @NonNull ClassificationStub stub,
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
