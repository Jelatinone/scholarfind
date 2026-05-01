package com.github.jelatinone.acquisition.interpreter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.github.jelatinone.acquisition.ContentInterpreter;
import com.github.jelatinone.acquisition.DetectedContent;
import com.github.jelatinone.acquisition.InterpretedContent;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HTMLContentInterpreter implements ContentInterpreter {
  static int PREVIEW_LIMIT = 500;

  @Override
  public boolean supports(@NonNull DetectedContent contentType) {
    return contentType.html();
  }

  @Override
  public @NonNull InterpretedContent interpret(byte[] source, @NonNull DetectedContent contentType) {
    String decodedSource = ContentInterpreter.decode(source, contentType.mediaEncoding());

    Document interpretedSource = Jsoup.parse(decodedSource);
    String normalizedSource = interpretedSource.text();

    return new InterpretedContent(
        source,
        normalizedSource,
        ContentInterpreter.preview(normalizedSource, PREVIEW_LIMIT));
  }

}
