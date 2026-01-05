package com.github.scholarfind.models.context;

import java.util.Map;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Timestamp;
import com.github.scholarfind.utility.Builder;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public record ContextDocument(
    Header header,
    Timestamp timestamp,

    String rawContext,
    String fingerprint) implements Document<ContextDocument> {
  public static final Long schemaVersion = 1L;

  public static ContextDocument deserialize(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public static ContextDocument deserialize(final @NotNull Message item) {
    // Not yet implemented...
    return null;
  }

  @Override
  public Map<String, MessageAttributeValue> attribute() {
    // Not yet implemented...
    return null;
  }

  @Override
  public Map<String, AttributeValue> itemize() {
    // Not yet implemented...
    return null;
  }

  @Override
  public String json() {
    // Not yet implemented...
    return null;
  }

  @Override
  public Builder<ContextDocument> toBuilder() {
    // Not yet implemented...
    return null;
  }
}
