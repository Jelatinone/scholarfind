package com.github.scholarfind.models.document.annotate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.models.document.DecisionType;
import com.github.scholarfind.models.document.Header;
import com.github.scholarfind.models.document.Location;
import com.github.scholarfind.models.document.Trace;

import lombok.NonNull;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public record AnnotateDocument(
    Header header,
    Trace trace,

    String organizationName,
    String scholarshipName,

    ChronoLocalDate openDate,
    ChronoLocalDate closeDate,

    Double awardAmount,

    Collection<Location> location,

    Collection<Activity> activities,
    Collection<SupplementalType> supplements,

    Collection<PursuedDegreeLevel> pursuedDegrees,
    Collection<EducationLevel> educationLevels) {

  private static ObjectMapper _mapper = new ObjectMapper()
      .configure(Feature.ALLOW_COMMENTS, true)
      .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
      .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static AnnotateDocument parse(final @NonNull Message message) throws MalformedURLException, IOException {
    Map<String, MessageAttributeValue> attributes = message.messageAttributes();
    String body = message.body();

    if (!attributes.containsKey("schemaVersion")) {
      throw new IOException("Failed to parse: invalid message metadata");
    }

    AnnotateDocument document;
    try {
      document = _mapper.readValue(body, AnnotateDocument.class);
    } catch (final JsonParseException exception) {
      document = repair(attributes);
    }
    return document;
  }

  public static AnnotateDocument repair(final @NonNull Map<String, MessageAttributeValue> attributes)
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

    AnnotateDocument document = new AnnotateDocument(header, trace, null, null, null, null, null, null, null, null,
        null, null);
    return document;
  }
}
