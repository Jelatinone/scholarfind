package com.github.scholarfind.models.document.search;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.models.document.DecisionType;
import com.github.scholarfind.models.document.Header;
import com.github.scholarfind.models.document.Trace;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import lombok.NonNull;

public record SearchDocument(
    Header header,
    Trace trace,
    Classification classification) {

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

  public static SearchDocument repair(final @NonNull Map<String, MessageAttributeValue> attributes)
      throws MalformedURLException {
    Long schemaVersion = Long.parseLong(attributes.get("schemaVersion").stringValue());
    UUID id = UUID.fromString(attributes.get("id").stringValue());

    Header header = new Header(schemaVersion, id);

    URL url = URI.create(attributes.get("url").stringValue()).toURL();
    URL parentUrl = URI.create(attributes.get("parentUrl").stringValue()).toURL();

    String reviewer = attributes.get("reviewer").stringValue();

    DecisionType decision = DecisionType.MALFORMED_DATA;

    Integer depth = Integer.valueOf(attributes.get("depth").stringValue());
    Integer attempt = Integer.valueOf(attributes.get("attempt").stringValue());

    ChronoLocalDate discoveredAt = ChronoLocalDate
        .from(ZonedDateTime.parse(attributes.get("discoveredAt").stringValue()));
    ChronoLocalDate reviewedAt = ChronoLocalDate
        .from(ZonedDateTime.parse(attributes.get("reviewedAt").stringValue()));

    Trace trace = new Trace(url, parentUrl, reviewer, decision, depth, attempt, discoveredAt, reviewedAt);

    Classification classification = new Classification(ClassificationType.OTHER, 1D);

    SearchDocument document = new SearchDocument(header, trace, classification);
    return document;
  }

  public Map<String, MessageAttributeValue> extract() {
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

  @Override
  public String toString() {
    return header().id().toString();
  }
}
