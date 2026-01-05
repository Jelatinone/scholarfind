package com.github.scholarfind.models.annotate;

import java.time.chrono.ChronoLocalDate;
import java.util.Collection;
import java.util.Map;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Location;
import com.github.scholarfind.models.Timestamp;
import com.github.scholarfind.models.Trace;
import com.github.scholarfind.utility.Builder;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public record AnnotateDocument(
    Header header,
    Trace trace,
    Timestamp timestamp,

    String organizationName,
    String scholarshipName,

    ChronoLocalDate openDate,
    ChronoLocalDate closeDate,

    Double awardAmount,

    Collection<Location> location,

    Collection<Activity> activities,
    Collection<SupplementalType> supplements,

    Collection<PursuedDegreeLevel> pursuedDegrees,
    Collection<EducationLevel> educationLevels) implements Document<AnnotateDocument> {
  public static final Long schemaVersion = 1L;

  public static AnnotateDocument deserialize(final @NotNull Map<String, AttributeValue> item) {
    // Not yet implemented...
    return null;
  }

  public static AnnotateDocument deserialize(final @NotNull Message item) {
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
  public Builder<AnnotateDocument> toBuilder() {
    // Not yet implemented...
    return null;
  }
}
