package com.github.jelatinone.model.scholarship;

import java.util.Set;

import com.github.jelatinone.model.publication.Publication;
import com.github.jelatinone.model.publication.PublicationHeader;
import com.github.jelatinone.model.scholarship.dossier.Description;
import com.github.jelatinone.model.scholarship.dossier.Requirement;

import lombok.NonNull;

public record ScholarshipPublication(

    @NonNull PublicationHeader archiveHeader,

    Set<Description<?>> itemDescriptions,

    String summary,
    String description,

    Set<Requirement<?>> applicationRequirements,
    Set<Requirement<?>> eligibilityRequirements

) implements Publication<ScholarshipPublication> {

  @Override
  public ScholarshipPublication withArchiveHeader(PublicationHeader header) {
    return new ScholarshipPublication(
        header,
        itemDescriptions(),
        summary(),
        description(),
        applicationRequirements(),
        eligibilityRequirements());
  }

}
