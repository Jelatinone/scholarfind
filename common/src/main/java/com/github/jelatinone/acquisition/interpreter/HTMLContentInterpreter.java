package com.github.jelatinone.acquisition.interpreter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.github.jelatinone.acquisition.ContentInterpreter;
import com.github.jelatinone.acquisition.DetectedContent;
import com.github.jelatinone.acquisition.InterpretedContent;
import com.github.jelatinone.models.content.ContentKind;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HTMLContentInterpreter implements ContentInterpreter {
  static int PREVIEW_LIMIT = 500;

  @Override
  public boolean supports(@NonNull DetectedContent contentType) {
    return contentType.contentKind() == ContentKind.HTML;
  }

  @Override
  public @NonNull InterpretedContent interpret(byte[] source, @NonNull DetectedContent contentType) {
    String decodedSource = ContentInterpreter.decode(source, contentType.characterEncoding());

    Document interpretedSource = Jsoup.parse(decodedSource);
    String normalizedSource = interpretedSource.text();

    return new InterpretedContent(
        source,
        normalizedSource,
        ContentInterpreter.preview(normalizedSource, PREVIEW_LIMIT));
  }

}
