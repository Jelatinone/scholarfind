package com.github.scholarfind.models.context;

import java.util.Map;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public record ContextDocument(
    String rawContext,
    String fingerprint) {

  public static ContextDocument parse(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public Map<String, AttributeValue> item() {
    // Not yet implemented...
    return null;
  }

}
