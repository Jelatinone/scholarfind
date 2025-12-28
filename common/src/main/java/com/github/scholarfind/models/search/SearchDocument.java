package com.github.scholarfind.models.search;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Trace;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import lombok.NonNull;

public record SearchDocument(
    Header header,
    Trace trace,
    Classification classification) {

  public static final Long schemaVersion = 1L;

  public static ObjectMapper _mapper = new ObjectMapper()
      .configure(Feature.ALLOW_COMMENTS, true)
      .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
      .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static SearchDocument parse(final @NonNull Message message) throws MalformedURLException, IOException,
      JsonParseException {
    Map<String, MessageAttributeValue> attributes = message.messageAttributes();
    String body = message.body();

    if (!attributes.containsKey("schemaVersion")) {
      throw new IOException("Failed to parse: invalid message metadata");
    }

    SearchDocument document;
    document = _mapper.readValue(body, SearchDocument.class);
    return document;
  }

  public static SearchDocument parse(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public Map<String, MessageAttributeValue> attribute() {
    Map<String, MessageAttributeValue> attributes = new HashMap<>();

    attributes.put("schemaVersion",
        MessageAttributeValue.builder().stringValue(header().schemaVersion().toString()).build());
    attributes.put("id",
        MessageAttributeValue.builder().stringValue(header().id().toString()).build());
    attributes.put("state",
        MessageAttributeValue.builder().stringValue(header().state().toString()).build());

    return attributes;
  }

  public Map<String, AttributeValue> itemize() {
    // Not yet implemented...
    return null;
  }

  @Override
  public String toString() {
    return String.format("search-document [%d]", header().id().toString().toString());
  }
}
