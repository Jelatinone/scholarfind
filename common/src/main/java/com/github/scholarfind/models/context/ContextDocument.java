package com.github.scholarfind.models.context;

import java.util.Map;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public record ContextDocument(
    String rawContext,
    String fingerprint) {
  public static final Long schemaVersion = 1L;

  public static ContextDocument deserialize(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public static ContextDocument deserialize(final @NotNull Message item) {
    // Not yet implemented...
    return null;
  }

  public Map<String, MessageAttributeValue> attribute() {
    // Not yet implemented...
    return null;
  }

  public Map<String, AttributeValue> itemize() {
    // Not yet implemented...
    return null;
  }

  public String json() {
    // Not yet implemented...
    return null;
  }
}
