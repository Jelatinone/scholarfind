package com.github.scholarfind.task.validation;

import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.task.SearchContext;
import com.github.scholarfind.validation.Capability;
import com.github.scholarfind.validation.Reason;
import com.github.scholarfind.validation.Validator;
import com.github.scholarfind.validation.ValidatorResult;

public class ClassificationValidator
    implements Validator<InvestigateDocument, SearchContext> {

  @Override
  public void validate(ValidatorResult record, SearchContext context) {
    InvestigateDocument retrieved = context.retrievedInvestigate();

    if (retrieved == null) {
      return;
    }

    if (retrieved.documentHeader().schemaVersion() != InvestigateDocument.schemaVersion) {
      record.deny(Capability.CLASSIFY, Reason.SCHEMA_MISMATCH);
    }

    var retrievedReviewedAt = retrieved.reviewedAt();
    if (retrievedReviewedAt != null &&
        retrievedReviewedAt.isAfter(context.reviewedAt().minusSeconds(30L * 24 * 60 * 60))) {

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
