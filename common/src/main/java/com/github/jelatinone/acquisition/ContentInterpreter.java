package com.github.jelatinone.acquisition;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.github.jelatinone.models.content.CharacterEncoding;

import lombok.NonNull;

public interface ContentInterpreter {
  boolean supports(@NonNull DetectedContent contentType);

  @NonNull
  InterpretedContent interpret(@NonNull byte[] source, @NonNull DetectedContent contentType);

  public static String decode(byte[] source, CharacterEncoding encoding) {
    Charset charset = StandardCharsets.UTF_8;
    if (encoding != null && encoding.canonicalName() != null) {
      try {
        charset = Charset.forName(encoding.canonicalName());
      } catch (Exception exception) {
        charset = StandardCharsets.UTF_8;
      }
    }
    return new String(source, charset);
  }

  public static String preview(String text, int upperBound) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String normalized = text.trim();
    return normalized.length() <= upperBound
        ? normalized
        : normalized.substring(0, upperBound);
  }
}
