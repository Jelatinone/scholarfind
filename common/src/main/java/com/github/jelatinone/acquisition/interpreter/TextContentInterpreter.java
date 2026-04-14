package com.github.jelatinone.acquisition.interpreter;

import com.github.jelatinone.acquisition.ContentInterpreter;
import com.github.jelatinone.acquisition.DetectedContent;
import com.github.jelatinone.acquisition.InterpretedContent;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TextContentInterpreter implements ContentInterpreter {

  static int PREVIEW_LIMIT = 500;

  @Override
  public boolean supports(@NonNull DetectedContent contentType) {
    return switch (contentType.contentKind()) {
      case PLAIN_TEXT, MARKDOWN, XML, JSON -> true;
      default -> false;
    };
  }

  @Override
  public @NonNull InterpretedContent interpret(byte[] source, @NonNull DetectedContent contentType) {
    String normalizedText = ContentInterpreter.decode(source, contentType.characterEncoding());
    return new InterpretedContent(
        source,
        normalizedText,
        ContentInterpreter.preview(normalizedText, PREVIEW_LIMIT));
  }

}
