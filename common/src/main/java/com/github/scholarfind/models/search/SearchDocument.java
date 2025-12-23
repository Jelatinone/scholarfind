package com.github.scholarfind.models.search;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Lifecycle;
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

  private static ObjectMapper _mapper = new ObjectMapper()
      .configure(Feature.ALLOW_COMMENTS, true)
      .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
      .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static SearchDocument parse(final @NonNull Message message) throws MalformedURLException, IOException {
    Map<String, MessageAttributeValue> attributes = message.messageAttributes();
    String body = message.body();

    if (!attributes.containsKey("schemaVersion")) {
      throw new IOException("Failed to parse: invalid message metadata");
    }

    SearchDocument document;
    try {
      document = _mapper.readValue(body, SearchDocument.class);
    } catch (final JsonParseException exception) {
      document = repair(attributes);
    }
    return document;
  }

  public static SearchDocument parse(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public static SearchDocument repair(final @NonNull Map<String, MessageAttributeValue> attributes)
      throws MalformedURLException {
    Long schemaVersion = Long.parseLong(attributes.get("schemaVersion").stringValue());
    UUID id = UUID.fromString(attributes.get("id").stringValue());
    Lifecycle state = Lifecycle.valueOf(attributes.get("state").stringValue());

    Header header = new Header(schemaVersion, id, state);

    URL url = URI.create(attributes.get("url").stringValue()).toURL();
    URL parentUrl = URI.create(attributes.get("parentUrl").stringValue()).toURL();

    String reviewer = attributes.get("reviewer").stringValue();

    Integer depth = Integer.valueOf(attributes.get("depth").stringValue());
    Integer attempt = Integer.valueOf(attributes.get("attempt").stringValue());

    ZonedDateTime discoveredAt = ZonedDateTime
        .from(ZonedDateTime.parse(attributes.get("discoveredAt").stringValue()));
    ZonedDateTime reviewedAt = ZonedDateTime
        .from(ZonedDateTime.parse(attributes.get("reviewedAt").stringValue()));

    Trace trace = new Trace(url, parentUrl, reviewer, depth, attempt, discoveredAt, reviewedAt);

    Classification classification = new Classification(ClassificationType.UNCLASSIFIED, 0D);

    SearchDocument document = new SearchDocument(header, trace, classification);
    return document;
  }

  public Map<String, MessageAttributeValue> attributes() {
    Map<String, MessageAttributeValue> attributes = new HashMap<>();

    attributes.put("schemaVersion",
        MessageAttributeValue.builder().stringValue(header().schemaVersion().toString()).build());
    attributes.put("id",
        MessageAttributeValue.builder().stringValue(header().id().toString()).build());

    attributes.put("url",
        MessageAttributeValue.builder().stringValue(trace().url().toString()).build());
    attributes.put("parentUrl",
        MessageAttributeValue.builder().stringValue(trace().parentUrl().toString()).build());

    attributes.put("reviewer",
        MessageAttributeValue.builder().stringValue(trace().reviewer().toString()).build());

    attributes.put("depth",
        MessageAttributeValue.builder().stringValue(trace().depth().toString()).build());
    attributes.put("attempt",
        MessageAttributeValue.builder().stringValue(trace().attempt().toString()).build());

    attributes.put("discoveredAt",
        MessageAttributeValue.builder().stringValue(trace().discoveredAt().toString()).build());
    attributes.put("reviewedAt",
        MessageAttributeValue.builder().stringValue(trace().discoveredAt().toString()).build());

    return attributes;
  }

  public Map<String, AttributeValue> item() {
    // Not yet implemented...
    return null;
  }

  @Override
  public String toString() {
    return String.format("search-document [%d]", header().id().toString().toString());
  }
}
