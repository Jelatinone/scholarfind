package com.github.scholarfind.task.validation;

import java.time.ZonedDateTime;

import com.github.scholarfind.models.search.Classification;
import com.github.scholarfind.models.search.SearchDocument;
import com.github.scholarfind.task.SearchContext;
import com.github.scholarfind.validation.*;

public class ClassificationValidator
    implements Validator<SearchDocument, SearchContext> {

  @Override
  public void validate(ValidatorResult record, SearchContext context) {
    SearchDocument retrieved = context.retreivedSearch();

    if (retrieved == null) {
      return;
    }

    switch (retrieved.header().state()) {
      case SUPERSEEDED, TOMBSTONED -> {
      }
      case CURRENT -> {
        record.deny(Capability.CLASSIFY, Reason.STATE_EXCEEDED);
      }
    }

    if (retrieved.header().schemaVersion() != SearchDocument.schemaVersion) {
      record.deny(Capability.CLASSIFY, Reason.SCHEMA_MISMATCH);
    }

    ZonedDateTime retrievedReviewedAt = retrieved.timestamp().reviewedAt();
    if (retrievedReviewedAt != null &&
        retrievedReviewedAt.isAfter(context.reviewedAt().minusDays(30))) { // TODO: <--- Needs configurability

      Classification classification = retrieved.classification();
      if (classification != null) {
        boolean confident = classification.contributions().values().stream()
            .anyMatch(value -> value >= context.classificationConfiguration()
                .dominanceConfiguration().minimumConfidence());

        if (confident) {
          record.deny(Capability.CLASSIFY, Reason.DOCUMENT_ERROR);
        }
      }
    }
  }
}