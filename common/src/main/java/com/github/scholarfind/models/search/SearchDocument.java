package com.github.scholarfind.models.search;

import java.util.Map;

import com.github.scholarfind.api.queue.QueueMessage;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Trace;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public record SearchDocument(
    Header header,
    Trace trace,
    Classification classification) {

  public static final Long schemaVersion = 1L;

  public static SearchDocument deserialize(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public static SearchDocument deserialize(final @NotNull QueueMessage item) {
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
