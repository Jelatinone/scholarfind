package com.github.scholarfind.models.content;

public enum CharacterEncoding {
  UTF_8("UTF-8"),
  UTF_16("UTF-16"),
  UTF_16LE("UTF-16LE"),
  UTF_16BE("UTF-16BE"),
  US_ASCII("US-ASCII"),
  ISO_8859_1("ISO-8859-1"),
  WINDOWS_1252("windows-1252"),
  OTHER(null);

  private final String canonicalName;

  CharacterEncoding(String canonicalName) {
    this.canonicalName = canonicalName;
  }

  public String canonicalName() {
    return canonicalName;
  }
}
