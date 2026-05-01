package com.github.jelatinone.acquisition;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.github.jelatinone.model.content.MediaEncoding;

import lombok.NonNull;

public interface ContentInterpreter {
  boolean supports(@NonNull DetectedContent contentType);

  @NonNull
  InterpretedContent interpret(byte[] source, @NonNull DetectedContent contentType);

  static String decode(byte[] source, MediaEncoding encoding) {
    Charset charset = StandardCharsets.UTF_8;
    if (encoding != null && encoding.getCanonicalName() != null) {
      try {
        charset = Charset.forName(encoding.getCanonicalName());
      } catch (Exception ignored) {
      }
    }
    return new String(source, charset);
  }

  static String preview(String text, int upperBound) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String normalized = text.trim();
    return normalized.length() <= upperBound
        ? normalized
        : normalized.substring(0, upperBound);
  }
}
