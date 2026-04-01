package com.github.jelatinone.models.content;

public enum ContentMediaType {
  TEXT_HTML("text/html"),
  APPLICATION_PDF("application/pdf"),
  TEXT_PLAIN("text/plain"),
  TEXT_MARKDOWN("text/markdown"),
  APPLICATION_XML("application/xml"),
  TEXT_XML("text/xml"),
  APPLICATION_JSON("application/json"),
  TEXT_JSON("text/json"),
  APPLICATION_OCTET_STREAM("application/octet-stream"),
  OTHER(null);

  private final String canonicalName;

  ContentMediaType(String canonicalName) {
    this.canonicalName = canonicalName;
  }

  public String canonicalName() {
    return canonicalName;
  }
}
