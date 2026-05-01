package com.github.jelatinone.acquisition;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

public record DetectedContent(
    MediaType mediaType,
    MediaEncoding mediaEncoding) {

  public boolean html() {
    return mediaType == MediaType.TEXT_HTML;
  }

  public boolean pdf() {
    return mediaType == MediaType.APPLICATION_PDF;
  }

  public boolean textLike() {
    return switch (mediaType == null ? MediaType.OTHER : mediaType) {
      case TEXT_PLAIN, TEXT_MARKDOWN, APPLICATION_XML, TEXT_XML, APPLICATION_JSON, TEXT_JSON -> true;
      default -> false;
    };
  }
}
